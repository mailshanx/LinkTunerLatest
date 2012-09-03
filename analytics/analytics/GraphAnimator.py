'''
Created on May 31, 2012

@author: shankar
'''
import paramiko as pm
import subprocess
import time
import copy
import ubigraph
import datetime

class GraphAnimator(object):
    '''
    Animates the main algorithm as a graph
    '''

    def __init__(self):
        '''
        Constructor
        '''
        pass
    def animateGraph(self, hostName):
        U=ubigraph.Ubigraph()
        U.clear()
        edge_list=[]
        node_set={}
        edge_count={}
        transition_node=None
        last_line_processed=None
        filename = self.drawGraph(U, edge_list, node_set, edge_count, transition_node, last_line_processed, hostName)
        return filename
    
    def get_value(self,record,param):
        assert(record[0]=='status' or record[0]=='history')
        pos_status= {'type':0, 'timestamp':1, 'bandit':2, 'alpha':3, 'beta':4, 'gittins_index':5, 'normalized_index':5}
        pos_history={'type':0, 'timestamp':1, 'bandit':2, 'banditID':3, 'result':4, 'BER':5, 'absolute_data_rate':6,
                     'alpha':7, 'beta':8, 'gittins_index':9, 'normalized_index':10,  
                     'MTYPE':11, 'DMODE':12, 'MPSK':13, 'Nc':14, 'Np':15, 'Nz':16, 'pktlen':17, 'FEC':18}
        if(record[0]=='status'):
            return record[pos_status[param]]
        elif(record[0]=='history'):
            return record[pos_history[param]]
    
    def _get_shape(self,record):
        #returns a shape based on a DMODE/MPSK combination
        if(record[0]=='history'):
            shape_dict={(1,2):'cone', (1,4):'cube', (2,2):'torus', (2,4):'sphere'}
            dmode_value=int(self.get_value(record,'DMODE'))
            mpsk_value=int(self.get_value(record,'MPSK'))
            return shape_dict[(dmode_value, mpsk_value)]
        else:
            return 'icosahedron'
     
    def get_shape(self, node_set, record):
        node_id=str(self.get_value(record, 'bandit'))
        if node_id in node_set:
            return node_set[node_id]
        else:
            shape = self._get_shape(record)
            if shape!='icosahedron':
                node_set[node_id]=shape
            return shape
        
     
    def drawGraph(self,U, edge_list, node_set, edge_count, transition_node, last_line_processed, hostName):        
        _host_name=hostName
        client = pm.SSHClient()
        client.load_system_host_keys()
        client.connect(hostname=_host_name, username='root',password='root')
        stdin, stdout, stderr = client.exec_command("cat /home/modem/logs/log-0.txt | egrep 'printLatestHistory|printBanditParamsStatus' | tr '\|' ' '")
        now = datetime.datetime.now()
        filename='pandan_trials_'+str(now.strftime("%Y-%m-%d_%H:%M"))+'.txt'
        fh=open(filename,'w')
        for items in stdout.readlines():
            current_time=int(items.strip().split()[0])
            if(last_line_processed is None or current_time > last_line_processed):
                last_line_processed=current_time
                fh.write(items)
        fh.close()
        refine_command="./refine.sh "+filename
        subprocess.call([refine_command], shell=True)
        #subprocess.call(["/home/shankar/Downloads/UbiGraph-alpha-0.2.4-Linux32-Ubuntu-8.04/bin/ubigraph_server"], shell=True)
        refined_filename=filename+'_refined.txt'
        fh=open(refined_filename)
        _fh=open(refined_filename)
        _fh.readline()
        print "edge_list init: ", edge_list
        for items in _fh.readlines():
            r1=fh.readline().strip().split()
            r2=items.strip().split()
            node_id_1=int(self.get_value(r1,'bandit')) / 10
            node_id_2=int(self.get_value(r2,'bandit')) / 10
            node_1=U.newVertex(id=node_id_1, label=str(node_id_1),shape=self.get_shape(node_set,r1), size=1, color='100')
            node_2=U.newVertex(id=node_id_2, label=str(node_id_2),shape=self.get_shape(node_set,r2), size=1, color='100')
            if(self.get_value(r1,'type')=='history' and self.get_value(r2,'type')=='status'):
                transition_node=copy.copy(node_1)
            if(self.get_value(r1,'type')=='status' and transition_node is not None):
                print "transition edge"
                U.newEdge(transition_node, node_1, spline=True, color='#11ccff', stroke='dashed')
            if(self.get_value(r1, 'type')=='history' and self.get_value(r2, 'type')=='history'):
                node_2.set(size=1.5, color='#ff0000')
                time.sleep(0.1)
                node_2.set(size=1, color='100')
                if((node_1.id,node_2.id) in edge_list):
                    print "edge exists!"
                    edge_count[(node_1.id,node_2.id)]+=1
                    _strength=1.0 - (edge_count[(node_1.id,node_2.id)] / max(edge_count.values()))
                    U.newEdge(node_1, node_2,spline=True, color="#C5892F", strength=_strength * 2.5, width = str(_strength*2) )
                else:
                    print "new edge!"
                    edge_list.append((node_1.id,node_2.id))
                    edge_count[(node_1.id,node_2.id)]=1
                    U.newEdge(node_1,node_2, arrow=True, color='#ff0000', width='1', strength=0.7)
            time.sleep(0.0)
        return refined_filename
