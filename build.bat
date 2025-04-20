@echo off
echo Building Remote Desktop System...

mkdir bin 2>nul

echo Compiling common classes...
javac -d bin src/common/*.java

echo Compiling server classes...
javac -d bin -cp bin src/server/*.java

echo Compiling client classes...
javac -d bin -cp bin src/client/*.java

echo Compiling main class...
javac -d bin -cp bin Main.java

echo Build complete. Running application...
java -cp bin Main

echo Done.