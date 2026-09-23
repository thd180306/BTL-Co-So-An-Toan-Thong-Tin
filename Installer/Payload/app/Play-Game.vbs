' Launch only the game
Option Explicit

Dim shell, fso, basePath
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
basePath = fso.GetParentFolderName(WScript.ScriptFullName)

shell.CurrentDirectory = basePath
shell.Run "javaw -jar """ & basePath & "\game\PixelChaser.jar""", 1, False
