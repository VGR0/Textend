Set WshShell = CreateObject("WScript.Shell" ) 

Set fso = CreateObject("Scripting.FileSystemObject")
Set thisfile = fso.GetFile(Wscript.ScriptFullName)
thisdir = fso.GetParentFolderName(thisfile) 

args = ""
If WScript.Arguments.Count > 0 Then
    ReDim argsarray(WScript.Arguments.Count-1)
    For i = 0 To WScript.Arguments.Count-1
      argsarray(i) = chr(34) & WScript.Arguments(i) & chr(34)
    Next
    args = " " & Join(argsarray," ")
End If

WshShell.Run thisdir & "\bin\javaw.exe " & _
 "-m net.pan.textend/net.pan.textend.Main " & args

Set args = Nothing
Set thisdir = Nothing
Set thisfile = Nothing
Set fso = Nothing
Set WshShell = Nothing 
