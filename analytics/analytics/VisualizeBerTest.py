'''
Created on May 15, 2012

@author: shankar
'''
import os
import string
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import subprocess
import numpy


def visualizeDmodeMpsk(data):
    fig1=plt.figure(figsize=(16,12))
    ax1=fig1.add_subplot(221,projection='3d')
    logic_array=data['Nc']==1024                        #numpy.logical_and(data['Nc']==1024,data['Np']<=200)
    ax1.plot(data[logic_array]['DMODE'],data[logic_array]['MPSK'],data[logic_array]['BER'],'bo',label='Nc=1024')
    logic_array=data['Nc']==512                         #numpy.logical_and(data['Nc']==1024,data['Np']>=800)
    ax1.plot(data[logic_array]['DMODE'],data[logic_array]['MPSK'],data[logic_array]['BER'],'r*',label='Nc=512')
    setLegendLabels(ax1,'DMODE','MPSK','BER','Nc = 1024,512')
    
    ax2=fig1.add_subplot(222,projection='3d')
    logic_array=data['Nc']==128                         #numpy.logical_and(data['Nc']==512,data['Np']<=200)
    ax2.plot(data[logic_array]['DMODE'],data[logic_array]['MPSK'],data[logic_array]['BER'],'bo',label='Nc=128')
    logic_array=data['Nc']==64                         #numpy.logical_and(data['Nc']==512,data['Np']>=800)
    ax2.plot(data[logic_array]['DMODE'],data[logic_array]['MPSK'],data[logic_array]['BER'],'r*',label='Nc=64')
    setLegendLabels(ax2,'DMODE','MPSK','BER','Nc = 128,64')

    ax3=fig1.add_subplot(223,projection='3d')
    logic_array=data['Nc']==1024#numpy.logical_and(data['Nc']==256,data['Np']<=200)
    ax3.plot(data[logic_array]['DMODE'],data[logic_array]['MPSK'],data[logic_array]['absolute_data_rate'],'bo',label='Nc=1024')
    logic_array=data['Nc']==512#numpy.logical_and(data['Nc']==512,data['Np']>=800)
    ax3.plot(data[logic_array]['DMODE'],data[logic_array]['MPSK'],data[logic_array]['absolute_data_rate'],'r*',label='Nc=512')
    setLegendLabels(ax3,'DMODE','MPSK','Data rate (Kbps)','Nc = 1024,512')

    ax4=fig1.add_subplot(224,projection='3d')
    logic_array=data['Nc']==128#numpy.logical_and(data['Nc']==128,data['Np']<=200)
    ax4.plot(data[logic_array]['DMODE'],data[logic_array]['MPSK'],data[logic_array]['absolute_data_rate'],'bo',label='Nc=128')
    logic_array=data['Nc']==64#numpy.logical_and(data['Nc']==128,data['Np']>=800)
    ax4.plot(data[logic_array]['DMODE'],data[logic_array]['MPSK'],data[logic_array]['absolute_data_rate'],'r*',label='Nc=64')
    setLegendLabels(ax4,'DMODE','MPSK','Data rate (Kbps)','Nc = 128,64')
    
    pass

def visualizeNcNp(data):
    fig1=plt.figure(figsize=(16,12))
    ax1=fig1.add_subplot(221,projection='3d')
    logic_array=numpy.logical_and(data['DMODE']==1,data['MPSK']==2)
    ax1.plot(data[logic_array]['Nc'],data[logic_array]['Np'],data[logic_array]['BER'],'bo',label='BPSK'); 
    logic_array=numpy.logical_and(data['DMODE']==1,data['MPSK']==4)
    ax1.plot(data[logic_array]['Nc'],data[logic_array]['Np'],data[logic_array]['BER'],'ro',label='QPSK'); 
    ax1.set_zlim([0,1])
    ax1.set_xlabel('Nc'); ax1.set_ylabel('Np'); ax1.set_zlabel('BER'); ax1.set_title('Time differential mode'); 
    ax1.set_yticks(range(0,1000,400)); ax1.set_zticks(numpy.arange(0,1.0,0.2))
    ax1.legend(loc='best')
    plt.savefig('image2.png')
    
    #fig2=plt.figure()
    ax2=fig1.add_subplot(222,projection='3d')
    logic_array=numpy.logical_and(data['DMODE']==2,data['MPSK']==2)
    ax2.plot(data[logic_array]['Nc'],data[logic_array]['Np'],data[logic_array]['BER'],'bo',label='BPSK');
    logic_array=numpy.logical_and(data['DMODE']==2,data['MPSK']==4)
    ax2.plot(data[logic_array]['Nc'],data[logic_array]['Np'],data[logic_array]['BER'],'ro',label='QPSK');
    ax2.set_zlim([0,1])
    ax2.set_xlabel('Nc'); ax2.set_ylabel('Np'); ax2.set_zlabel('BER'); ax2.set_title('Frequency differential mode');
    ax2.set_yticks(range(0,1000,400)); ax2.set_zticks(numpy.arange(0,1.0,0.2))
    ax2.legend(loc='best')

    ax3=fig1.add_subplot(223,projection='3d')
    logic_array=numpy.logical_and(data['DMODE']==1,data['MPSK']==2)
    ax3.plot(data[logic_array]['Nc'],data[logic_array]['Np'],data[logic_array]['absolute_data_rate'],'bo',label='BPSK');
    logic_array=numpy.logical_and(data['DMODE']==1,data['MPSK']==4)
    ax3.plot(data[logic_array]['Nc'],data[logic_array]['Np'],data[logic_array]['absolute_data_rate'],'ro',label='QPSK');
#    ax3.set_zlim([0,1])
    ax3.set_xlabel('Nc'); ax3.set_ylabel('Np'); ax3.set_zlabel('Data rate (Kbps)'); ax3.set_title('Time differential mode');
    ax3.set_yticks(range(0,1000,400)); #ax3.set_zticks(numpy.arange(0,1.0,0.2))
    ax3.legend(loc='best')
    
    ax4=fig1.add_subplot(224,projection='3d')
    logic_array=numpy.logical_and(data['DMODE']==2,data['MPSK']==2)
    ax4.plot(data[logic_array]['Nc'],data[logic_array]['Np'],data[logic_array]['absolute_data_rate'],'bo',label='BPSK');
    logic_array=numpy.logical_and(data['DMODE']==2,data['MPSK']==4)
    ax4.plot(data[logic_array]['Nc'],data[logic_array]['Np'],data[logic_array]['absolute_data_rate'],'ro',label='QPSK');
#    ax3.set_zlim([0,1])
    ax4.set_xlabel('Nc'); ax4.set_ylabel('Np'); ax4.set_zlabel('Data rate (Kbps)'); ax4.set_title('Frequency differential mode');
    ax4.set_yticks(range(0,1000,400)); #ax3.set_zticks(numpy.arange(0,1.0,0.2))
    ax4.legend(loc='best')
#    fig1.savefig('image2.png') 

def PlotCodedDataRates(data):
    coded_data_rate=[]
    mul_factor=[]
    for item in data:
        if item['BER'] > 0.15:
            coded_data_rate.append(0.0)
            mul_factor.append(0.0)
        elif item['BER'] > 0.09 and item['BER'] <= 0.15:
            coded_data_rate.append(0.15*item['absolute_data_rate'])
            mul_factor.append(0.15)
        elif item['BER'] > 0.04 and item['BER'] <= 0.09:
            coded_data_rate.append(0.33*item['absolute_data_rate'])
            mul_factor.append(0.33)
        elif item['BER'] >= 0.0:
            coded_data_rate.append(0.5*item['absolute_data_rate'])
            mul_factor.append(0.5)
        print "item['BER'] = ", item['BER'], "appended mul_factor ", mul_factor[-1]
    coded_data_rate=numpy.array(coded_data_rate)
    logic_array=numpy.logical_and(data['DMODE']==1,data['MPSK']==2)
    fig1=plt.figure(figsize=(10,12))
    ax1=fig1.add_subplot(211,projection='3d')
    ax1.plot(data[logic_array]['Nc'],data[logic_array]['Np'],coded_data_rate[logic_array],'bo', label='BPSK')
    logic_array=numpy.logical_and(data['DMODE']==1,data['MPSK']==4)
    ax1.plot(data[logic_array]['Nc'],data[logic_array]['Np'],coded_data_rate[logic_array],'ro', label='QPSK')
    ax1.set_xlabel('Nc'); ax1.set_ylabel('Np'); ax1.set_zlabel('Coded data rate'); ax1.set_title('Time differential mode');
    ax1.legend(loc='best')
    
    ax2=fig1.add_subplot(212,projection='3d')
    logic_array=numpy.logical_and(data['DMODE']==2,data['MPSK']==2)
    ax2.plot(data[logic_array]['Nc'],data[logic_array]['Np'],coded_data_rate[logic_array],'bo', label='BPSK')
    logic_array=numpy.logical_and(data['DMODE']==2,data['MPSK']==4)
    ax2.plot(data[logic_array]['Nc'],data[logic_array]['Np'],coded_data_rate[logic_array],'ro', label='QPSK')
    ax2.set_xlabel('Nc'); ax2.set_ylabel('Np'); ax2.set_zlabel('coded data rate'); ax2.set_title('Frequency differential mode');
    ax2.legend(loc='best')
    print "verification"
    print coded_data_rate[logic_array]
    print "verification complete"
    for items in (coded_data_rate[logic_array], data[logic_array]['BER'], data[logic_array]['absolute_data_rate']):
        print  items[0], " ", items[1], " ", items[2]
    
    logic_array=numpy.logical_and(logic_array, data['Nc']==1024)
    print "logic_array = ", logic_array
    fig=plt.figure()
    ax=fig.add_subplot(111)
    ax.plot(data[logic_array]['Np'], coded_data_rate[logic_array], 'ro')
    print "coded_data_rate[logic_array] = ", coded_data_rate[logic_array]
    print "abs data rate = ", data[logic_array]['absolute_data_rate']
    print "mul_factor = ", mul_factor
    print "BERs = ", data['BER']
#    print coded_data_rate[logic_array], data[logic_array]['BER'], data[logic_array]['absolute_data_rate']
    pass

def setLegendLabels(ax,xLabel,yLabel,zLabel,title,legendLocation='best'):
    ax.set_xlabel(xLabel);ax.set_ylabel(yLabel);ax.set_zlabel(zLabel);ax.set_title(title);ax.legend(loc=legendLocation);
    pass

if __name__ == '__main__':
    os.chdir('/home/shankar/Desktop')
    data_format={'names':('bandit','playCount','result','BER',  'absolute_data_rate',   'MTYPE','DMODE','MPSK','Nc',  'Np',   'Nz','PKT_LEN','FEC'),
                'formats':('S10',     'f4',    'S10',   'f4',          'f4',             'f4',   'f4',  'f4', 'f4',  'f4',   'f4',  'f4',   'f4')}
    raw_data=numpy.loadtxt('BER_test_refined_tx-att-22_14062012.txt', dtype=data_format)
    filtered_data=raw_data[raw_data['BER']<0.50]
    visualizeNcNp(filtered_data)
#    visualizeDmodeMpsk(filtered_data)
    PlotCodedDataRates(filtered_data)
    plt.show()
#    subprocess.call(["cat BER_test.txt | grep printGrandPlay | sed 's/^.*FINE\|//g' | sed 's/^.*ment//g' \
#    | tr \| ' ' > ber_test_results_python.txt ; cat ber_test_results_python.txt"], shell=True)
    pass