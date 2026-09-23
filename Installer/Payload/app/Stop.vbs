' Stop all PixelChaser processes
Option Explicit

Dim shell
Set shell = CreateObject("WScript.Shell")

' Kill all java processes (our services use java.exe)
' Skip any javaw (user might be running other Java GUIs)
shell.Run "cmd /c taskkill /F /IM java.exe /T", 0, True

' Show confirmation
MsgBox "Pixel Chaser services stopped.", 64, "Pixel Chaser"
