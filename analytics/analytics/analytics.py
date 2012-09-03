'''
Created on Apr 22, 2012

@author: shankar
'''
import os
import string
import matplotlib.pyplot as plt
import subprocess
import copy
import numpy as np
import matplotlib.mlab as mlab
import GraphAnimator


def plotBerDataRate(data):
    fig1=plt.figure(figsize=(16,12))
    
    ax1=fig1.add_subplot(211)
    average_data_rate=np.array([np.mean(data['absolute_data_rate'][:z]) for z in range(1,len(data['absolute_data_rate']))])
    ax1.plot(data['absolute_data_rate'], 'b*',label='inst data rate')
    ax1.plot(average_data_rate,'ro',label='avg data rate')
    ax1.set_title('data rate (Kbps)')
    ax1.legend(loc='best')
#    ax1.annotate('moved modems', xy=(42,6.6), xytext=(15,5.8), arrowprops=dict(facecolor='black', shrink=0.05),)
#    ax1.annotate('decreased SNR', xy=(98,6.8), xytext=(70,5.8), arrowprops=dict(facecolor='black', shrink=0.05),)
#    ax1.annotate('increased SNR again', xy=(199,8.1), xytext=(160,7.0), arrowprops=dict(facecolor='black', shrink=0.05),)
    
    ax2=fig1.add_subplot(212)
    ax2.plot(data['BER'],'bo')
    ax2.set_title('BER')
    plt.show()

def plotBanditParams(data):
    fig1=plt.figure(figsize=(16,12))
        
#    ax1=fig1.add_subplot(411)
#    ax1.plot(data['alpha'], 'ro'); 
#    ax1.set_title(r'$\alpha$')
#    
#    ax2=fig1.add_subplot(412)
#    ax2.plot(data['beta'], 'bo')
#    ax2.set_title(r'$        \beta$')
    
    ax3=fig1.add_subplot(211)
    ax3.plot(data['gittins_index'], 'ro')
    ax3.set_title(r'$           \nu$')
    
#    ax4=fig1.add_subplot(614)
#    ax4.plot(data['gittins_index_norm'], 'bo')
#    ax4.set_title(r'$\nu_n$')

    ax5=fig1.add_subplot(212)
    ax5.plot(data['banditID'],'bo')
    ax5.set_title('banditID')
 
         
def plotOfdmParams(data):
    fig1=plt.figure(figsize=(16,12))
    
    ax1=fig1.add_subplot(411)
    ax1.plot(data['DMODE'],'ro')
    ax1.set_title('DMODE')
    
    ax2=fig1.add_subplot(412)
    ax2.plot(data['MPSK'],'bo')
    ax2.set_title('MPSK')
    
    ax3=fig1.add_subplot(413)
    ax3.plot(data['Nc'],'ro')
    ax3.set_title('Nc')
    
    ax4=fig1.add_subplot(414)
    ax4.plot(data['Np'],'bo')
    ax4.set_title('Np')
    plt.show()
    pass

if __name__ == '__main__':
#    os.chdir('/home/shankar/Desktop/Research/modem-sim2/logs')
#    subprocess.call(["cat log-0.txt | grep printGrandPlay | sed 's/^.*FINE\|//g' | sed 's/^.*ment//g' \
#    > experiment_results.txt ; cat experiment_results.txt"], shell=True)
    host_name='192.168.0.22'
    graph_animator=GraphAnimator.GraphAnimator()
    filename=graph_animator.animateGraph(host_name)
    command_to_refine="cat "+filename+"| grep 'history' | sed 's/^.*history//' > "+filename+"_3d_plots.txt"
    plot_filename=filename+"_3d_plots.txt"
    subprocess.call([command_to_refine], shell=True)
    data_format={'names':('timestamp',  'bandit','banditID','result','BER',  'absolute_data_rate',  'alpha',    'beta', 'gittins_index', 'gittins_index_norm',  'MTYPE','DMODE','MPSK','Nc',  'Np',   'Nz','PKT_LEN','FEC'),
                 'formats':( 'S10',      'S10',     'f4',    'S10',   'f4',          'f4',           'f4',       'f4',       'f4',              'f4',             'f4',   'f4',  'f4', 'f4',  'f4',   'f4',  'f4',   'f4')}
    raw_data=np.loadtxt(plot_filename, dtype=data_format)
    raw_data=mlab.rec_append_fields(raw_data, 'time_as_int', np.array([int(times) for times in raw_data['timestamp']]) )
    raw_data=mlab.rec_append_fields(raw_data, 'bandit_as_int', np.array([int(bandits) for bandits in raw_data['bandit']]) )
    filtered_data=raw_data[raw_data['BER']<0.8] 
    plotBanditParams(filtered_data)
    plotOfdmParams(filtered_data)
    plotBerDataRate(filtered_data)
    plt.show()
    
    
    
    