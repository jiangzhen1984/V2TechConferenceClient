from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
from com.android.chimpchat.hierarchyviewer import HierarchyViewer
from com.android.monkeyrunner.easy import EasyMonkeyDevice, By 


def findByButtonAndPress(strId):
	print "start to find and press id :" + strId
	node = device.getViewById(strId)
	location = node.getLocation()
	print strId+    "   ["+str(location.getCenter()[0])+" , "+str(location.getCenter()[1])+"]"
	device.touch(location.getCenter()[0], location.getCenter()[1],MonkeyDevice.DOWN_AND_UP)


device = MonkeyRunner.waitForConnection()
device.removePackage("com.v2tech");
device.installPackage("./V2ConferenceClient.apk")
runComponent = "com.v2tech/com.v2tech.view.StartupActivity"
device.startActivity(runComponent)
MonkeyRunner.sleep(5)
print "========> installed and started application\n"




device.type("test1")
device.press("DPAD_DOWN", MonkeyDevice.DOWN_AND_UP)
MonkeyRunner.sleep(0.5)
device.type("111111")
print "========> input test accound\n"

#find setting button and press
findByButtonAndPress("show_setting")
MonkeyRunner.sleep(1)

device.type("211.157.170.57")
device.press("DPAD_DOWN", MonkeyDevice.DOWN_AND_UP)
device.type("5123")

findByButtonAndPress('ip_setting_save')
MonkeyRunner.sleep(0.5)

MonkeyRunner.alert("start to login", "login", "ok")
MonkeyRunner.sleep(1)
findByButtonAndPress("sign_in_button")
MonkeyRunner.sleep(5)



