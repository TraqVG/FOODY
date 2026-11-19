$root='C:\GitHub\Smart-Restaurant-Ordering\backend'
$p=Start-Process python -ArgumentList @('-m','uvicorn','main:app','--host','127.0.0.1','--port','8000') -WorkingDirectory $root -RedirectStandardOutput "$root\server.out.log" -RedirectStandardError "$root\server.err.log" -PassThru
Write-Output ("STARTED_PID="+$p.Id)
