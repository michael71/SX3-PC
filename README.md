# SX3-PC
Selectrix (Model Railroad) Monitor and Control SW (PC, JAVA) via RS232 interface, see <a href="http://www.oscale.net">oscale.net/sx3</a> and <a href="http://opensx.net">opensx.net/sx3</a>.

##Das System:
Selectrix™ ist ein relativ einfaches Digitalsystem (in der ursprünglichen Version) – es gibt 112 Adressen (auch „Kanäle“ genannt) mit jeweils 8 Bit Daten, die regelmäßig wiederholt werden. Daher kann man den Gesamtzustand des Systems jeweils gut auf einem Bildschirm darstellen, es gibt bereits einige sogenannte „SX Monitor“ Programme. Jede diese 112 Adressen kann entweder zur Loksteuerung, Weichen- oder Signalsteuerung oder für Rückmeldezwecke (Belegtmelder/Sensoren) verwendet werden - es gibt also keinen separaten Rückmeldebus. DAS IST ALLES! Einfach und zweckmäßig.

##Das SX3 Programm: 
Das Programm entstand aus dem Wunsch heraus, einen Selectrix-"Monitor" auch unter Linux verwenden zu können. Wie bei allen ähnlichen Programmen gibt es die Gesamtübersicht, den „SX Monitor“ - mit der Besonderheit, dass alle Kanäle, die von „0“ verschiedene Daten haben, gelb hinterlegt sind - und wenn sich gegenüber der letzten Sekunde die Daten geändert haben, werden sie Orange hinterlegt. (diese SX Monitor Fenster gibt es nur, wenn das Interface ein Rautenhaus 825SLX oder ein anderes modernes Interface ist, nicht beim alten Trix™ Interface 66824, da dort alle Adressen aktiv aktualisiert werden müssen (polling)). 

Hierbei wird die übliche SX-Darstellung gewählt mit Bit 1 bis Bit 8, wobei das niederwertige zuerst gezeigt wird.

<img src="http://www.oscale.net/images/sx3-monitor.png" />