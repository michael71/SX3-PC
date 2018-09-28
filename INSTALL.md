# SX3-PC Installieren

SX3 ist eine Selectrix (Model Railroad) Monitor und Control SW (PC, JAVA) via RS232 Interface, siehe auch
	http://www.oscale.net/sx3 
	http://opensx.net/sx3
	 
(in diesem Verzeichnis auch zu finden: die bewährte Version 0.97, läuft auch mit dem ganz alten Selectrix Interface 66824)

## Installieren unter Linux

1. Java sollte vorhanden sein, sonst installieren. 
zB mit: 
sudo apt-get install openjdk-7-jre 

2. sudo apt-get install librxtx-java
( mehr zum rxtx-setup in Ubuntu )

3. sx3-dist-x.y.zip entzippen und java -jar sx3.jar ausführen

4. Setup aufrufen, serielle Schnittstelle wählen und Programm neu starten

5. Falls keine seriellen Schnittstellen zu sehen sind: der aktuelle User
muss in der Dialout Gruppe sein!  
     ( fixen mit:   sudo usermod -a -G dialout username )

Das Programm läuft übrigens auch auf dem RaspberryPI, Sie brauchen dort z.B. einen USB-auf-RS232 Adapter, um den "Raspi" mit der RS232 Schnittstelle des SLX 825 zu verbinden.

## unter Windows installieren

!! Achtung: in Zukunft wird nur noch LINUX unterstützt, da es für Windows bereits genug andere, ähnliche Programme gibt!

1. Java installieren  ( http://www.java.com ) 
Achtung: wenn Java eine 64bit Version ist, muss auch rxtx eine 64bit Version sein!!

2. leider hat das Standard Java keine Unterstützung für die RS232 Schnittstelle, daher müssen Sie RXTX installieren
	a. download der Binary (zum Beispiel rxtx-2.1-7-bins-r2.zip ) 
              von    http://rxtx.qbang.org/wiki/index.php/Download
	b. RXTX (wir brauchen nur die Windows DLL rxtxSerial.dll) entzippen
	c. die rxtxSerial.dll ins Verzeichnis ...\Java\jre6\bin verschieben

3. sx3-dist-x.y.zip in ein Verzeichnis entzippen

4. Doppelclick auf sx3.jar startet das Programm
   (oder in dem Verzeichnis, in dem sx3.jar liegt, java -jar sx3.jar ausführen)

5. Setup aufrufen, serielle Schnittstelle wählen und Programm neu starten


## SX3-PC benutzen

für die Version 0.97-0.99, siehe 
http://www.oscale.net/sx3-anleitung.pdf
sx3-anleitung (in diesem Verzeichnis)

<img src="http://www.oscale.net/images/sx3-monitor.png" />

Feel free to use the software - however, use at your own risk. There is no warranty of any kind, but contact me if you found bugs.

