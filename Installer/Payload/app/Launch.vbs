' ============================================================
' Pixel Chaser - Silent Launcher
' Starts services, opens dual portal windows, launches game
' ============================================================
Option Explicit

Dim shell, fso, basePath, javaBin, javawBin
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
basePath = fso.GetParentFolderName(WScript.ScriptFullName)

shell.CurrentDirectory = basePath

' 0. Clean up any previous stale java.exe services to free ports 5000 & 8080
On Error Resume Next
shell.Run "cmd /c taskkill /F /IM java.exe /T", 0, True
On Error Goto 0
WScript.Sleep 1000

' Detect bundled runtime if installed, else fallback to system PATH
javaBin = "java"
javawBin = "javaw"
If fso.FileExists(basePath & "\..\runtime\bin\javaw.exe") Then
    javawBin = """" & basePath & "\..\runtime\bin\javaw.exe"""
    javaBin = """" & basePath & "\..\runtime\bin\java.exe"""
ElseIf fso.FileExists(basePath & "\runtime\bin\javaw.exe") Then
    javawBin = """" & basePath & "\runtime\bin\javaw.exe"""
    javaBin = """" & basePath & "\runtime\bin\java.exe"""
End If

' 1. Start Demo Controller (hidden background service)
shell.Run "cmd /c """ & basePath & "\service-controller.bat""", 0, False
WScript.Sleep 2500

' 2. Start Web Backend (hidden background service)
shell.Run "cmd /c """ & basePath & "\service-web.bat""", 0, False
WScript.Sleep 2000

' 3. Open Attacker C2 Operations Console in a new window
If fso.FileExists(basePath & "\open-portals.vbs") Then
    shell.Run "wscript.exe """ & basePath & "\open-portals.vbs""", 0, False
Else
    shell.Run "http://127.0.0.1:8080/c2.html", 1, False
End If
WScript.Sleep 1500

' 4. Start game with javaw (arcade game window appears)
shell.Run javawBin & " -jar """ & basePath & "\game\PixelChaser.jar""", 1, False
