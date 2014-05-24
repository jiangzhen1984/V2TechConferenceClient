from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
from com.android.chimpchat.hierarchyviewer import HierarchyViewer
from com.android.monkeyrunner.easy import EasyMonkeyDevice, By 


def findByButtonAndPress(strId):
	hview = device.getHierarchyViewer()
	node = hview.findViewById("id/"+strId)
	location = hview.getAbsolutePositionOfView(node)
	print strId+    "   x:"+str(location.x)+"   y:"+str(location.y)
	device.touch(location.x+10, location.y+10,MonkeyDevice.DOWN_AND_UP)


device = MonkeyRunner.waitForConnection()
device.removePackage("V2ConferenceClient.apk");
device.installPackage("./V2ConferenceClient.apk")
runComponent = "com.v2tech/com.v2tech.view.StartupActivity"
device.startActivity(runComponent)
MonkeyRunner.sleep(5)
print "========> installed and started application\n"




device.type("test1")
device.press("DPAD_DOWN", MonkeyDevice.DOWN_AND_UP)
MonkeyRunner.sleep(0.5)
device.type("11111")
print "========> input test accound\n"

#find setting button and press
findByButtonAndPress("show_setting")
MonkeyRunner.sleep(1)

device.type("211.157.170.57")
device.press("DPAD_DOWN", MonkeyDevice.DOWN_AND_UP)
device.type("5123")
easy_device = EasyMonkeyDevice(device)
easy_device.touch(By.id('id/ip_setting_save'), MonkeyDevice.DOWN_AND_UP)
MonkeyRunner.sleep(0.5)
#press back key to hide soft keyboard
device.press("KEYCODE_BACK", MonkeyDevice.DOWN_AND_UP)

MonkeyRunner.alert("start to login", "login", "ok")
MonkeyRunner.sleep(0.5)
findByButtonAndPress("sign_in_button")
MonkeyRunner.sleep(5)



