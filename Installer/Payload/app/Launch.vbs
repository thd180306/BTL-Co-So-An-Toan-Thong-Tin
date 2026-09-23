' ============================================================
' Pixel Chaser - Silent Launcher
' ============================================================
Option Explicit

Dim shell, fso, basePath, javaBin, javawBin
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
basePath = fso.GetParentFolderName(WScript.ScriptFullName)

shell.CurrentDirectory = basePath

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

' 1. Start Demo Controller (hidden)
shell.Run "cmd /c """ & basePath & "\service-controller.bat""", 0, False
WScript.Sleep 3000

' 2. Start Web Backend (hidden)
shell.Run "cmd /c """ & basePath & "\service-web.bat""", 0, False
WScript.Sleep 2500

' 3. Open browser to Recovery Portal
shell.Run "http://127.0.0.1:8080/", 1, False
WScript.Sleep 1500

' 4. Start game with javaw (no console window)
shell.Run javawBin & " -jar """ & basePath & "\game\PixelChaser.jar""", 1, False
