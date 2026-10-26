"""Выпуск новой версии Ежедневника на GitHub и проверка обновления на эмуляторе.

Выпуск (версию сначала поднять в app/build.gradle.kts и закоммитить):

    python tools/release.py publish --note "Что нового, пункт 1" --note "Пункт 2"

Что делает: проверяет, что всё закоммичено, прогоняет модульные тесты, собирает выпускной APK, проверяет,
что он подписан ключом пользователя, пишет update.json (номер версии, ссылка, размер, SHA-256, что нового),
сохраняет файл соответствия R8 в release-mappings/<версия>, копию APK — в «Загрузки», ставит метку v<версия>,
отправляет main и метку на GitHub и создаёт выпуск с APK и update.json. Приложение на телефоне найдёт его
по ссылке releases/latest/download/update.json. С --dry-run — всё, кроме отправки на GitHub.

Проверка обновления на эмуляторе (отладочная сборка ищет обновления на http://localhost:8765, порт пробрасывается
на компьютер через adb reverse):

    python tools/release.py serve-debug

Собирает отладочный APK текущей версии, кладёт его с update.json в dist/debug, пробрасывает порт 8765
с эмулятора на компьютер и раздаёт файлы по http.
Порядок: поставить на эмулятор отладочную сборку предыдущей версии, поднять версию, запустить serve-debug,
открыть приложение — должна появиться плашка «Доступна версия …».
"""

import argparse
import hashlib
import http.server
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REPO = "snax1k/ezhednevnik"
# Отпечаток сертификата ключа пользователя (CN=Chekki). Другим ключом подписанный APK телефон не поставит поверх.
EXPECTED_CERT_SHA256 = "6a1647c6d41aca508f49d0cf94f075ba59b7cd2ba93bbecaf3e304ab5175248a"
DEBUG_PORT = 8765


def run(*args, capture=False, cwd=ROOT):
    print("›", " ".join(str(a) for a in args), flush=True)
    result = subprocess.run([str(a) for a in args], cwd=cwd, check=True, text=True,
                            capture_output=capture, encoding="utf-8")
    return result.stdout if capture else None


def fail(message):
    print(f"\nОшибка: {message}", file=sys.stderr)
    sys.exit(1)


def gradle(*tasks):
    env = os.environ.copy()
    studio_jdk = Path(r"C:\Program Files\Android\Android Studio\jbr")
    if "JAVA_HOME" not in env and studio_jdk.exists():
        env["JAVA_HOME"] = str(studio_jdk)
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    print("› gradlew", " ".join(tasks), flush=True)
    subprocess.run([str(wrapper), *tasks, "-q"], cwd=ROOT, check=True, env=env)


def version():
    text = (ROOT / "app" / "build.gradle.kts").read_text(encoding="utf-8")
    code = re.search(r"versionCode\s*=\s*(\d+)", text)
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    min_sdk = re.search(r"minSdk\s*=\s*(\d+)", text)
    if not (code and name and min_sdk):
        fail("не нашёл versionCode, versionName или minSdk в app/build.gradle.kts")
    return int(code.group(1)), name.group(1), int(min_sdk.group(1))


def build_tools():
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("LOCALAPPDATA", "") + r"\Android\Sdk")
    versions = sorted((sdk / "build-tools").iterdir(), key=lambda p: [int(x) for x in re.findall(r"\d+", p.name)])
    return versions[-1]


def apk_cert_sha256(apk):
    tools = build_tools()
    apksigner = tools / ("apksigner.bat" if os.name == "nt" else "apksigner")
    output = run(apksigner, "verify", "--print-certs", apk, capture=True)
    match = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})", output)
    return match.group(1) if match else None


def apk_version_code(apk):
    tools = build_tools()
    aapt2 = tools / ("aapt2.exe" if os.name == "nt" else "aapt2")
    badging = run(aapt2, "dump", "badging", apk, capture=True)
    match = re.search(r"versionCode='(\d+)'", badging)
    return int(match.group(1)) if match else None


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as file:
        for chunk in iter(lambda: file.read(1 << 16), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_update_json(path, code, name, min_sdk, apk, url, notes):
    info = {
        "versionCode": code,
        "versionName": name,
        "apk": url,
        "sha256": sha256(apk),
        "size": apk.stat().st_size,
        "minSdk": min_sdk,
        "notes": notes,
    }
    path.write_text(json.dumps(info, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return info


def gh():
    found = shutil.which("gh") or r"C:\Program Files\GitHub CLI\gh.exe"
    if not Path(found).exists() and not shutil.which("gh"):
        fail("не найдена программа gh")
    return found


def publish(args):
    code, name, min_sdk = version()
    tag = f"v{name}"
    notes = [n.strip() for n in args.note if n.strip()]
    if not notes:
        fail("нужен хотя бы один пункт «Что нового»: --note \"…\"")

    # Выпуск — только из закоммиченного кода; служебные файлы Android Studio не в счёт
    dirty = [line for line in run("git", "status", "--porcelain", capture=True).splitlines()
             if line[3:] and not line[3:].startswith(".idea/")]
    if dirty:
        fail("есть незакоммиченные изменения:\n" + "\n".join(dirty))
    if run("git", "tag", "--list", tag, capture=True).strip():
        fail(f"метка {tag} уже есть — поднимите версию в app/build.gradle.kts")

    if not args.skip_tests:
        gradle("testDebugUnitTest")
    gradle("assembleRelease")

    built = ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"
    cert = apk_cert_sha256(built)
    if cert != EXPECTED_CERT_SHA256:
        fail(f"APK подписан не тем ключом ({cert}). Проверьте CHEKKI_STORE_FILE в ~/.gradle/gradle.properties")
    if apk_version_code(built) != code:
        fail("номер версии в APK не совпадает с app/build.gradle.kts")

    dist = ROOT / "dist" / name
    dist.mkdir(parents=True, exist_ok=True)
    apk = dist / f"Ezhednevnik-{name}.apk"
    shutil.copyfile(built, apk)
    url = f"https://github.com/{REPO}/releases/download/{tag}/{apk.name}"
    info = write_update_json(dist / "update.json", code, name, min_sdk, apk, url, notes)
    print(json.dumps(info, ensure_ascii=False, indent=2))

    # Файл соответствия R8 нужен, чтобы расшифровать ошибку с телефона
    mapping = ROOT / "release-mappings" / name
    mapping.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ROOT / "app" / "build" / "outputs" / "mapping" / "release" / "mapping.txt", mapping / "mapping.txt")
    downloads = Path.home() / "Downloads"
    if downloads.exists():
        shutil.copyfile(apk, downloads / apk.name)

    if args.dry_run:
        print(f"\nПробный прогон: всё готово в {dist}, на GitHub ничего не отправлено.")
        return

    run("git", "tag", "-a", tag, "-m", f"Ежедневник {name}")
    run("git", "push", "origin", "main", tag)
    body = "## Что нового\n\n" + "\n".join(f"- {n}" for n in notes) + (
        "\n\nОбновление приходит в приложение само: на главном экране появится плашка «Доступна версия»."
        f"\nВручную — скачать `{apk.name}` и открыть на телефоне."
    )
    run(gh(), "release", "create", tag, apk, dist / "update.json",
        "--repo", REPO, "--title", f"Ежедневник {name}", "--notes", body, "--latest")
    print(f"\nВыпуск {tag} опубликован: https://github.com/{REPO}/releases/tag/{tag}")


def serve_debug(args):
    code, name, min_sdk = version()
    gradle("assembleDebug")
    dist = ROOT / "dist" / "debug"
    dist.mkdir(parents=True, exist_ok=True)
    apk = dist / "app-debug.apk"
    shutil.copyfile(ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk", apk)
    url = f"http://localhost:{DEBUG_PORT}/{apk.name}"
    notes = [n.strip() for n in args.note if n.strip()] or ["Проверка обновления на эмуляторе"]
    info = write_update_json(dist / "update.json", code, f"{name}-debug", min_sdk, apk, url, notes)
    print(json.dumps(info, ensure_ascii=False, indent=2))
    adb = Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk" / "platform-tools" / "adb.exe"
    run(adb if adb.exists() else "adb", "reverse", f"tcp:{DEBUG_PORT}", f"tcp:{DEBUG_PORT}")
    handler = lambda *a, **kw: http.server.SimpleHTTPRequestHandler(*a, directory=str(dist), **kw)
    print(f"\nРаздаю {dist} на http://127.0.0.1:{DEBUG_PORT} (Ctrl+C — остановить)", flush=True)
    http.server.ThreadingHTTPServer(("127.0.0.1", DEBUG_PORT), handler).serve_forever()


def main():
    parser = argparse.ArgumentParser(description="Выпуск Ежедневника")
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("publish", help="собрать и опубликовать выпуск на GitHub")
    p.add_argument("--note", action="append", default=[], help="пункт «Что нового» (можно несколько)")
    p.add_argument("--dry-run", action="store_true", help="всё, кроме отправки на GitHub")
    p.add_argument("--skip-tests", action="store_true", help="не прогонять модульные тесты")
    p.set_defaults(func=publish)
    s = sub.add_parser("serve-debug", help="раздать отладочный APK для проверки обновления на эмуляторе")
    s.add_argument("--note", action="append", default=[])
    s.set_defaults(func=serve_debug)
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
