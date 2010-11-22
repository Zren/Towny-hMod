javac .\src\*.java  -cp "cp/Minecraft_Mod.jar";"cp/minecraft_server.jar" -Xlint -d bin
cd bin
jar cfe ..\Towny.jar Towny *.class
pause