#!/usr/bin/python

from __future__ import absolute_import, print_function, unicode_literals
from optparse import OptionParser, make_option
import os
import errno
import sys
import socket
import uuid
import dbus
import dbus.service
import dbus.mainloop.glib
import mraa
import time
import json
import pyupm_grove
import pyupm_grovemoisture
import pyupm_buzzer
import pyupm_servo
import pyupm_th02
import pyupm_i2clcd

#Analog
pinSound = 0
pinMoisture = 1
pinLight = 2
pinUV = 3
#Digital
pinButton = 0
pinEncoder1 = 2
pinEncoder2 = 3
pinBuzzer = 8#4
pinRelay = 5
pinServo = 6
pinPIR = 7
#IIC
addrTempHumi = 0x40
addrLCD = 0x3E
addrRGB = 0x62

#Devices
moisture = pyupm_grovemoisture.GroveMoisture(pinMoisture)
light = pyupm_grove.GroveLight(pinLight)
UV = mraa.Aio(pinUV)

button = pyupm_grove.GroveButton(pinButton)
Encoder1 = pyupm_grove.GroveButton(pinEncoder1)
Encoder2 = pyupm_grove.GroveButton(pinEncoder2)
buzzer = pyupm_buzzer.Buzzer(pinEncoder2)
relay = pyupm_grove.GroveRelay(pinRelay)
servo = pyupm_servo.Servo(pinServo)
PIR = mraa.Gpio(pinPIR)


tempHumi = pyupm_th02.TH02()
LCD = pyupm_i2clcd.Jhd1313m1(0, addrLCD, addrRGB)

def init():
	PIR.dir(mraa.DIR_IN)
	LCD.setCursor(0,0)
	LCD.setColor(53, 39, 249)
	LCD.backlightOn()
	LCD.autoscrollOn()
	LCD.write(str("Running BT"))
	LCD.setCursor(1,0)
	LCD.write(str("Socket Server"))

		# if relay.isOn() == True:
		#	 relay.off()
		# else:
		#	 relay.on()
		# buzzer.playSound(i, 100)
		# i = i+1
		# if i>9:
		#	 i=0;
		# servo.setAngle(j)
		# j = j + 10
		# if j >= 160:
		#	 j = 0

def printSensor():
	localtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime()) 
	print("Time: "+localtime)
	print("light: "+(str)(light.value())+" lux")
	print("light_raw: "+(str)(light.raw_value()))
	print("moisture: "+(str)(moisture.value()))
	print("UV: "+(str)(UV.readFloat()))
	print("temp: "+(str)(tempHumi.getTemperature())+" C")
	print("humi: "+(str)(tempHumi.getHumidity())+" %")
	print("PIR: "+(str)(PIR.read()))
	print("button: "+(str)(button.value()))
	print(" ")

def getJSON():
	data = {
		"data": {
			"TimeStamp": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime()),
			"Moisture": moisture.value(),
			"Light": light.value(),
			"Temp": tempHumi.getTemperature(),
			"Humi": tempHumi.getHumidity(),
			"UV": UV.readFloat(),
			"PIR": PIR.read()
		}
	}
	json_str = json.dumps(data)
	return json_str

try:
	from gi.repository import GObject
except ImportError:
	import gobject as GObject

class Profile(dbus.service.Object):
	fd = -1

	@dbus.service.method("org.bluez.Profile1",
					in_signature="", out_signature="")
	def Release(self):
		print("Release")
		mainloop.quit()

	@dbus.service.method("org.bluez.Profile1",
					in_signature="", out_signature="")
	def Cancel(self):
		print("Cancel")

	@dbus.service.method("org.bluez.Profile1",
				in_signature="oha{sv}", out_signature="")
	def NewConnection(self, path, fd, properties):
		self.fd = fd.take()
		print("NewConnection(%s, %d)" % (path, self.fd))


		server_sock = socket.fromfd(self.fd, socket.AF_UNIX, socket.SOCK_STREAM)
		server_sock.setblocking(1)
		#server_sock.send("Send \"start\" to recive current sensor data in JSON format")

		try:
			while True:
				data = server_sock.recv(1024)
				print("Got: %s" % data)
				if data == "start":
					print("Sending sensor data")
					printSensor()
					server_sock.send("%s" % getJSON())
		except IOError:
			pass

		server_sock.close()
		print("all done")



	@dbus.service.method("org.bluez.Profile1",
				in_signature="o", out_signature="")
	def RequestDisconnection(self, path):
		print("RequestDisconnection(%s)" % (path))

		if (self.fd > 0):
			os.close(self.fd)
			self.fd = -1


if __name__ == '__main__':
	init()
	dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)

	bus = dbus.SystemBus()

	manager = dbus.Interface(bus.get_object("org.bluez",
				"/org/bluez"), "org.bluez.ProfileManager1")

	option_list = [
			make_option("-C", "--channel", action="store",
					type="int", dest="channel",
					default=None),
			]

	parser = OptionParser(option_list=option_list)

	(options, args) = parser.parse_args()

	options.uuid = "1101"
	options.psm = "3"
	options.role = "server"
	options.name = "Edison Device"
	options.service = "sensor JSON service"
	options.path = "/media/sdcard"
	options.auto_connect = False
	options.record = ""

	profile = Profile(bus, options.path)

	mainloop = GObject.MainLoop()

	opts = {
			"AutoConnect" : options.auto_connect,
		}

	if (options.name):
		opts["Name"] = options.name

	if (options.role):
		opts["Role"] = options.role

	if (options.psm is not None):
		opts["PSM"] = dbus.UInt16(options.psm)

	if (options.channel is not None):
		opts["Channel"] = dbus.UInt16(options.channel)

	if (options.record):
		opts["ServiceRecord"] = options.record

	if (options.service):
		opts["Service"] = options.service

	if not options.uuid:
		options.uuid = str(uuid.uuid4())

	manager.RegisterProfile(options.path, options.uuid, opts)

	mainloop.run()



