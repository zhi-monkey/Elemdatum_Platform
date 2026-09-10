::windows
@REM SET CGO_ENABLED=0
@REM SET GOOS=windows
@REM SET GOARCH=amd64
@REM go build -o controller-server.exe main.go

::linux amd64
SET CGO_ENABLED=0
SET GOOS=linux
SET GOARCH=amd64
go build -o daemon_amd64 main.go

::linux aarch64
@REM SET CGO_ENABLED=0
@REM SET GOOS=linux
@REM SET GOARCH=arm64
@REM go build -o daemon_aarch64 main.go

::linux arm
@REM SET CGO_ENABLED=0
@REM SET GOOS=linux
@REM SET GOARCH=arm
@REM go build -o daemon_arm main.go

