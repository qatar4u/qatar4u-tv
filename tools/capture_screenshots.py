"""Capture the actual release APK on a clean Android emulator (no user data)."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

OUT = Path('screenshots')
OUT.mkdir(exist_ok=True)
RES = ET.parse('app/src/main/res/values-ar/strings.xml').getroot()
LABEL = {node.get('name'): node.text for node in RES.findall('string')}
SIZE = [1920, 1080]


def adb(*args):
    return subprocess.check_output(['adb', *args], text=True).strip()


def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/q4u-window.xml')
    xml = adb('shell', 'cat', '/sdcard/q4u-window.xml')
    result = list(ET.fromstring(xml).iter('node'))
    bounds = [list(map(int, re.findall(r'\d+', node.get('bounds', '')))) for node in result]
    valid = [b for b in bounds if len(b) == 4]
    if valid:
        SIZE[:] = [max(b[2] for b in valid), max(b[3] for b in valid)]
    return result


def find(label):
    for node in nodes():
        if label in (node.get('text'), node.get('content-desc')):
            values = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
            if len(values) == 4 and values[2] > values[0] and values[3] > values[1]:
                return values
    return None


def click(label, scroll=False):
    for _ in range(8 if scroll else 3):
        bounds = find(label)
        if bounds:
            x1, y1, x2, y2 = bounds
            adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
            time.sleep(1)
            return
        if scroll:
            # Navigation is the right-hand column in Arabic; never scroll the content pane.
            adb('shell', 'input', 'swipe', str(int(SIZE[0]*0.88)), str(int(SIZE[1]*0.85)), str(int(SIZE[0]*0.88)), str(int(SIZE[1]*0.30)), '450')
        else:
            time.sleep(1)
    raise AssertionError(f'UI element not found: {label}')


def capture(name):
    time.sleep(1)
    adb('shell', 'screencap', '-p', f'/sdcard/{name}.png')
    adb('pull', f'/sdcard/{name}.png', str(OUT / f'{name}.png'))
    print(f'Captured {name}')


adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
adb('shell', 'settings', 'put', 'system', 'user_rotation', '1')
adb('shell', 'wm', 'size', '1080x1920')
adb('shell', 'wm', 'density', '240')
adb('shell', 'am', 'start', '-n', 'net.qatar4u.tv/.MainActivity')
time.sleep(5)
capture('home')
click(LABEL['settings'])
click(LABEL['s_look'], scroll=True)
assert find(LABEL['card_reset']), 'Appearance controls did not render'
capture('appearance')
click(LABEL['s_downloads'], scroll=True)
assert find(LABEL['keypad_clear']) and find(LABEL['keypad_confirm']), 'Downloader keypad missing'
capture('downloader')
click(LABEL['s_security'], scroll=True)
assert find(LABEL['keypad_clear']) and find(LABEL['keypad_confirm']), 'PIN keypad missing'
capture('pin-keypad')
click(LABEL['s_about'], scroll=True)
assert find('Qatar4u'), 'Canonical publisher name missing'
capture('about')
