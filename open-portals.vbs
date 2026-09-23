' ============================================================
' Pixel Chaser - Dual-Window Portal Launcher
' Opens Victim Portal and Attacker C2 Console in 2 SEPARATE WINDOWS
' ============================================================
Option Explicit

Dim shell, fso, rid, victimUrl, attackerUrl, browserExe, flag

Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Optional parameter: recoveryId
rid = ""
If WScript.Arguments.Count > 0 Then
    rid = Trim(WScript.Arguments(0))
End If

victimUrl = "http://127.0.0.1:8080/"
attackerUrl = "http://127.0.0.1:8080/c2.html"

If rid <> "" Then
    victimUrl = "http://127.0.0.1:8080/?rid=" & rid
    attackerUrl = "http://127.0.0.1:8080/c2.html?rid=" & rid
End If

' Detect default browser executable
Function GetBrowserExe()
    Dim cmd, p1, p2, candidate, pid
    cmd = ""
    On Error Resume Next
    pid = shell.RegRead("HKCU\Software\Microsoft\Windows\Shell\Associations\UrlAssociations\http\UserChoice\ProgId")
    If pid <> "" Then
        cmd = shell.RegRead("HKCR\" & pid & "\shell\open\command\")
    End If
    On Error Goto 0

    If cmd <> "" Then
        If Left(cmd, 1) = """" Then
            p2 = InStr(2, cmd, """")
            If p2 > 2 Then
                candidate = Mid(cmd, 2, p2 - 2)
                If fso.FileExists(candidate) Then
                    GetBrowserExe = candidate
                    Exit Function
                End If
            End If
        Else
            p1 = InStr(cmd, " ")
            If p1 > 0 Then
                candidate = Left(cmd, p1 - 1)
            Else
                candidate = cmd
            End If
            If fso.FileExists(candidate) Then
                GetBrowserExe = candidate
                Exit Function
            End If
        End If
    End If

    ' Fallbacks for common Windows browsers
    Dim fallbacks, f
    fallbacks = Array( _
        "C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe", _
        "C:\Program Files\Google\Chrome\Application\chrome.exe", _
        "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe", _
        shell.ExpandEnvironmentStrings("%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"), _
        "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe", _
        "C:\Program Files\Microsoft\Edge\Application\msedge.exe" _
    )
    For Each f In fallbacks
        If fso.FileExists(f) Then
            GetBrowserExe = f
            Exit Function
        End If
    Next
    GetBrowserExe = ""
End Function

browserExe = GetBrowserExe()

If browserExe <> "" Then
    If InStr(LCase(browserExe), "firefox") > 0 Then
        flag = "-new-window"
    Else
        flag = "--new-window"
    End If

    ' 1. Launch Window 1 — Victim Portal in its own window
    shell.Run """" & browserExe & """ " & flag & " """ & victimUrl & """", 1, False
    WScript.Sleep 1000

    ' 2. Launch Window 2 — Attacker C2 Dashboard in its own window
    shell.Run """" & browserExe & """ " & flag & " """ & attackerUrl & """", 1, False
Else
    ' Fallback to standard URL associations if no browser executable detected
    shell.Run victimUrl, 1, False
    WScript.Sleep 1000
    shell.Run attackerUrl, 1, False
End If
