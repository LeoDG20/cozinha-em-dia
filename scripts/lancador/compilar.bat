@echo off
rem Recompila o CozinhaEmDia.exe a partir de Lancador.cs, usando o compilador do .NET Framework que acompanha o Windows.
cd /d "%~dp0"
set CSC=%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe
if not exist "%CSC%" set CSC=%WINDIR%\Microsoft.NET\Framework\v4.0.30319\csc.exe
"%CSC%" /nologo /target:exe /optimize+ /codepage:65001 /out:..\..\CozinhaEmDia.exe Lancador.cs
