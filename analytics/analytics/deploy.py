#!/usr/bin/env python
'''
Created on Jun 11, 2012

@author: shankar
'''
import paramiko as pm
import SCPClient
import os
import subprocess
import telnetlib
import time
import os


def createSSHClient(_host_name):
    client = pm.SSHClient()
    client.load_system_host_keys()
    client.connect(hostname=_host_name, username='root',password='root')
    return client

def transfer(_host_name, path_name_src, path_name_dest):
    SSHClient_1=createSSHClient(_host_name)
    SCPClient_1=SCPClient.SCPClient(SSHClient_1.get_transport())
    SCPClient_1.put(files=path_name_src, remote_path=path_name_dest)

def restartModem(_host_name, _port_no):
    tn1=telnetlib.Telnet(_host_name, _port_no)
    time.sleep(1)    
    ptr=tn1.open(_host_name, _port_no)
    time.sleep(1)
    tn1.write("atz"+ "\n")
    tn1.close()
    
def loadAgents(_host_name, _port_no):
    tn1=telnetlib.Telnet(_host_name, _port_no)
    time.sleep(1)
    tn1.open(_host_name, _port_no)
    time.sleep(1)
    tn1.write("AT/sb:org.arl.modem.linktuner.SmartBoy \n")
    time.sleep(1)
    tn1.write("AT/org.arl.modem.linktuner.SmartBoyCommands \n")
    pass

def startTuner(_host_name, _port_no):
    tn1=telnetlib.Telnet(_host_name, _port_no)
    time.sleep(1)
    tn1.open(_host_name, _port_no)
    time.sleep(1)
    tn1.write("at~sinit \n")
    tn1.close()    
    pass

def startBertest(_host_name, _port_no):
    tn1=telnetlib.Telnet(_host_name, _port_no)
    time.sleep(1)
    tn1.open(_host_name, _port_no)
    time.sleep(1)
    tn1.write("at~sbertest \n")
    tn1.close()    
    pass


if __name__ == '__main__':
    host1='192.168.0.21'
    host2="192.168.0.22"
    port=5100
    
    
    pwd = os.getcwd()
    path_to_project=os.path.join(pwd, '../../')

    linktuner_path_src=os.path.join(path_to_project, 'build/linkTuner.jar')
    linktuner_path_dest='/home/modem/java'
#    modem_path_src='/home/shankar/Dropbox/Latest Research/LinkTunerLatest/lib/modem.jar'
    modem_path_src=os.path.join(path_to_project, 'lib/modem.jar')
    modem_path_dest='/home/modem/java'
#    logging_path_src='/home/shankar/Dropbox/Latest Research/LinkTunerLatest/logging.properties'
    logging_path_src=os.path.join(path_to_project, 'logging.properties')
    logging_path_dest='/home/modem/etc'
#    startup_path_src='/home/shankar/Dropbox/Latest Research/LinkTunerLatest/startup.atc'
    startup_path_src=os.path.join(path_to_project,'startup.atc')
    startup_path_dest='/home/modem/etc'
#    ltconfig_src='/home/shankar/Dropbox/Latest Research/LinkTunerLatest/linktuner.config'
    ltconfig_src=os.path.join(path_to_project, 'linktuner.config')
    ltconfig_dst='/home/modem'
    
    print "updating build"
    os.chdir(path_to_project)
    subprocess.call(['ant jar'], shell=True)    
    
    print "transferring files"
    transfer(host1, linktuner_path_src, linktuner_path_dest)
    transfer(host1, modem_path_src, modem_path_dest)
    transfer(host1, logging_path_src, logging_path_dest)
    transfer(host1, startup_path_src, startup_path_dest)
    transfer(host1, ltconfig_src, ltconfig_dst)
    
    transfer(host2, linktuner_path_src, linktuner_path_dest)
    transfer(host2, modem_path_src, modem_path_dest)
    transfer(host2, logging_path_src, logging_path_dest)
    transfer(host2, startup_path_src, startup_path_dest)
    transfer(host2, ltconfig_src, ltconfig_dst)
    
    print "restarting modems"
    restartModem(host1, port)
    restartModem(host2, port)
    time.sleep(10)#don't mess with these timings. changing them makes the host dump a stacktrace. but if the host crashes, twiddling these might help
    
    print "loading agents"
    loadAgents(host1, port)
    loadAgents(host2, port)
    time.sleep(14)#don't mess with these timings. changing them makes the host dump a stacktrace. but if the host crashes, twiddling these might help
    
#    print "starting tuner"
#    startTuner(host2, port)
    
    print "starting bertest"
    startBertest(host2, port)
    
    









    