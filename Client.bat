chcp 866
echo off
cls
set /p host="Enter server ip: "
set /p port="Enter server port: "
java -cp jade.jar;classes jade.Boot -container -host %host% -port %port% -agents client1:Client("men.txt","women.txt")
pause