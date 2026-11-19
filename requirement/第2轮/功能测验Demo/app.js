const { createApp, ref, computed } = Vue;
createApp({
  setup() {
    const message = ElementPlus.ElMessage;
    const view=ref('buyer'), scene=ref('sale'), status=ref('sale'), helpVisible=ref(false);
    const makeBuyers=()=>[
      {id:1,code:'DEMO001',name:'演示买家甲',phone:'13800000001',state:'waiting',submitted:'10:00:00'},
      {id:2,code:'DEMO002',name:'演示买家乙',phone:'13800000002',state:'waiting',submitted:'10:02:00'},
      {id:3,code:'DEMO003',name:'演示买家丙',phone:'13800000003',state:'waiting',submitted:'10:04:00'}
    ];
    const buyers=ref(makeBuyers()),codeInput=ref('DEMO002'),selectedId=ref(2),logs=ref([]);
    const contact=ref({name:'演示买家乙',phone:'13800000002'}),newBuyer=ref({name:'',phone:''});
    const loggedIn=ref(true),password=ref('123456');
    const passwordForm=ref({old:'',next:'',confirm:''}),loginForm=ref({username:'admin',password:''});
    const waiting=computed(()=>buyers.value.filter(x=>x.state==='waiting'));
    const activeBuyer=computed(()=>buyers.value.find(x=>x.state==='trading'));
    const selected=computed(()=>buyers.value.find(x=>x.id===selectedId.value&&x.state!=='cancelled'));
    const queueRows=computed(()=>buyers.value.filter(x=>x.state!=='cancelled').slice().sort((a,b)=>(b.state==='trading')-(a.state==='trading')));
    const statusLabel=computed(()=>({sale:'在售',frozen:'手动冻结',trading:'交易中'})[status.value]);
    const statusType=computed(()=>({sale:'success',frozen:'warning',trading:'warning'})[status.value]);
    const productHint=computed(()=>({sale:'商品正在接收新意向，等待者可修改或撤销。',frozen:'已暂停新意向，原等待队列保留。解冻后恢复接单。',trading:'商品处于交易冻结，禁止新意向；其他等待者仍可修改资料。'})[status.value]);
    const sceneHint=computed(()=>({sale:'体验修改信息与排位保持',frozen:'体验冻结后双方允许的操作',otherTrade:'当前查询的是等待买家',ownTrade:'当前查询的是交易买家'})[scene.value]);
    function record(text){logs.value.unshift({time:new Date().toLocaleTimeString('zh-CN',{hour12:false}),text});}
    function position(b){return b.state==='trading'?'—':b.state==='waiting'?'第 '+(waiting.value.findIndex(x=>x.id===b.id)+1)+' 位':'—';}
    function buyerState(b){return ({waiting:'排队中',trading:'交易中',cancelled:'已撤销'})[b.state];}
    function fillContact(){if(selected.value)contact.value={name:selected.value.name,phone:selected.value.phone};}
    function chooseBuyer(b){codeInput.value=b.code;selectedId.value=b.id;fillContact();}
    function queryBuyer(){
      const found=buyers.value.find(x=>x.code===codeInput.value.trim().toUpperCase()&&x.state!=='cancelled');
      if(!found){selectedId.value=null;return message.warning('没有找到有效的演示口令码');}
      chooseBuyer(found);
    }
    function saveContact(){
      const b=selected.value;if(!b||b.state!=='waiting')return message.warning('只有等待中的买家可以修改');
      if(!contact.value.name.trim()||!contact.value.phone.trim())return message.warning('请填写姓名和联系电话');
      b.name=contact.value.name.trim();b.phone=contact.value.phone.trim();
      record(b.code+' 修改联系信息，口令码、提交时间和排位保持');message.success('联系信息已保存，原排位保持不变');
    }
    async function withdraw(){
      const b=selected.value;if(!b||b.state!=='waiting')return;
      try{await ElementPlus.ElMessageBox.confirm('撤销后该演示口令码失效，后续买家依次前移。','撤销购买意向',{confirmButtonText:'撤销意向',cancelButtonText:'保留意向'});}catch{return;}
      b.state='cancelled';record(b.code+' 撤销意向，后续排位更新');selectedId.value=null;message.success('意向已撤销');
    }
    function submitIntent(){
      if(status.value!=='sale')return message.warning('当前不接收新意向');
      if(!newBuyer.value.name.trim()||!newBuyer.value.phone.trim())return message.warning('请填写演示姓名和电话');
      const id=Math.max(0,...buyers.value.map(x=>x.id))+1;
      const b={id,code:'DEMO'+String(id).padStart(3,'0'),name:newBuyer.value.name.trim(),phone:newBuyer.value.phone.trim(),state:'waiting',submitted:new Date().toLocaleTimeString('zh-CN',{hour12:false})};
      buyers.value.push(b);chooseBuyer(b);newBuyer.value={name:'',phone:''};record(b.code+' 提交意向，加入队尾');message.success('已获得口令码 '+b.code);
    }
    async function freeze(){
      if(!loggedIn.value||status.value!=='sale')return;
      try{await ElementPlus.ElMessageBox.confirm('冻结后停止接收新意向，已有队列与口令码保留。','手动冻结',{confirmButtonText:'确认冻结',cancelButtonText:'取消'});}catch{return;}
      status.value='frozen';record('卖家手动冻结商品，等待队列保留');message.success('已手动冻结');
    }
    function unfreeze(){if(!loggedIn.value||status.value!=='frozen')return;status.value='sale';record('卖家解除手动冻结，恢复接收新意向');message.success('已恢复在售');}
    function startTrade(){
      if(!loggedIn.value||status.value!=='sale'||!waiting.value.length)return;
      const b=waiting.value[0];b.state='trading';status.value='trading';record('与队首 '+b.name+' 开始模拟交易');message.success('队首进入交易，其他买家继续等待');
    }
    function endTrade(){if(!loggedIn.value)return;const b=activeBuyer.value;if(b)b.state='waiting';status.value='sale';record('结束模拟交易，恢复演示队列（场景辅助操作）');}
    function changePassword(){
      if(passwordForm.value.old!==password.value)return message.warning('当前演示密码不正确');
      if(!passwordForm.value.next)return message.warning('请输入新的演示密码');
      if(passwordForm.value.next!==passwordForm.value.confirm)return message.warning('两次新密码不一致');
      password.value=passwordForm.value.next;passwordForm.value={old:'',next:'',confirm:''};loggedIn.value=false;loginForm.value.password='';
      record('演示密码已修改，旧卖家会话失效，需要重新登录');message.success('密码已修改，请使用新密码重新登录');
    }
    function login(){if(loginForm.value.username!=='admin'||loginForm.value.password!==password.value)return message.warning('演示账号或密码不正确');loggedIn.value=true;loginForm.value.password='';record('使用当前演示密码重新登录');message.success('已重新登录');}
    function applyScene(){
      buyers.value=makeBuyers();selectedId.value=2;codeInput.value='DEMO002';status.value=scene.value==='frozen'?'frozen':scene.value==='sale'?'sale':'trading';
      if(status.value==='trading')buyers.value[0].state='trading';
      if(scene.value==='ownTrade'){selectedId.value=1;codeInput.value='DEMO001';}
      newBuyer.value={name:'',phone:''};fillContact();record('载入场景：'+({sale:'在售',frozen:'手动冻结',otherTrade:'他人交易中',ownTrade:'本人交易中'})[scene.value]);
    }
    function resetDemo(){scene.value='sale';applyScene();view.value='buyer';password.value='123456';loggedIn.value=true;passwordForm.value={old:'',next:'',confirm:''};loginForm.value={username:'admin',password:''};logs.value=[];message.success('已恢复初始演示数据');}
    return {view,scene,status,helpVisible,buyers,codeInput,selected,contact,newBuyer,loggedIn,passwordForm,loginForm,waiting,activeBuyer,queueRows,statusLabel,statusType,productHint,sceneHint,logs,position,buyerState,fillContact,chooseBuyer,queryBuyer,saveContact,withdraw,submitIntent,freeze,unfreeze,startTrade,endTrade,changePassword,login,applyScene,resetDemo,record};
  }
}).use(ElementPlus).mount('#app');
