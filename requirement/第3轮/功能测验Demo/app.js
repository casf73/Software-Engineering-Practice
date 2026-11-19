const { createApp, ref, computed, watch } = Vue;
createApp({
 setup() {
  const message=ElementPlus.ElMessage;
  const view=ref('browse'), search=ref(''), categoryFilter=ref(null), page=ref(1), customerSearch=ref('');
  const seedCategories=()=>[
   {id:1,name:'数码配件',parentId:null},{id:2,name:'键盘',parentId:1},{id:3,name:'耳机',parentId:1},
   {id:4,name:'学习生活',parentId:null},{id:5,name:'图书',parentId:4},{id:6,name:'照明',parentId:4},
  ];
  const pictures=(kind)=>[1,2,3].map(i=>`./assets/${kind}-${i}.svg`);
  const seedProducts=()=>[
   {id:101,name:'机械键盘',price:128,stock:3,categoryId:2,status:'在售',images:pictures('keyboard'),description:'<h3>办公与学习</h3><p><strong>九成新机械键盘</strong>，适合日常办公。</p><ul><li>支持有线连接</li><li>附原装连接线</li></ul>',queue:[{name:'演示买家甲',phone:'13800000001',code:'KEY001'},{name:'演示买家乙',phone:'13800000002',code:'KEY002'}]},
   {id:102,name:'软件工程学习书籍',price:38,stock:1,categoryId:5,status:'在售',images:pictures('book'),description:'<h3>课程学习用书</h3><p>书页整洁，<strong>少量铅笔批注</strong>，可当面查看。</p>',queue:[{name:'演示买家丙',phone:'13800000003',code:'BOOK001'}]},
   {id:103,name:'护眼台灯',price:58,stock:2,categoryId:6,status:'在售',images:pictures('lamp'),description:'<p><strong>宿舍阅读与办公</strong>均可使用。</p><ul><li>亮度可调</li><li>可折叠收纳</li></ul>',queue:[]},
   {id:104,name:'有线头戴耳机',price:69,stock:4,categoryId:3,status:'在售',images:pictures('headphone'),description:'<p>学习、听课使用，佩戴舒适，附收纳袋。</p>',queue:[{name:'演示买家丁',phone:'13800000004',code:'EAR001'}]},
   {id:105,name:'无线办公键盘',price:96,stock:2,categoryId:2,status:'在售',images:[pictures('keyboard')[1]],description:'<p>轻薄<strong>办公</strong>键盘，桌面收纳方便。</p>',queue:[]},
   {id:106,name:'算法学习手册',price:25,stock:1,categoryId:5,status:'在售',images:[],description:'<p>算法基础与练习记录，无商品图片的展示示例。</p>',queue:[]},
   {id:107,name:'备用阅读台灯',price:42,stock:0,categoryId:6,status:'下架',images:[pictures('lamp')[1]],description:'<p>零库存示例，可在商品管理中体验补货上架。</p>',queue:[]},
  ];
  const seedCustomers=()=>[
   {id:1,username:'buyer01',password:'demo12345',phone:'13800000001',defaultLocation:'学校南门',created:'2026-10-24'},
   {id:2,username:'buyer02',password:'demo12345',phone:'13800000002',defaultLocation:'图书馆入口',created:'2026-10-24'},
  ];
  const seedOrders=()=>[
   {id:'D3001',customerId:1,productName:'机械键盘',price:128,location:'学校南门',status:'排队中',date:'2026-10-24 10:00'},
   {id:'D3002',customerId:1,productName:'软件工程学习书籍',price:38,location:'教学楼门口',status:'交易成功',date:'2026-10-24 09:30'},
   {id:'D3003',customerId:1,productName:'护眼台灯',price:58,location:'学校南门',status:'已撤销',date:'2026-10-24 09:00'},
   {id:'D3004',customerId:2,productName:'有线头戴耳机',price:69,location:'图书馆入口',status:'交易成功',date:'2026-10-24 10:15'},
   {id:'D3005',customerId:2,productName:'算法学习手册',price:25,location:'东门',status:'排队中',date:'2026-10-24 10:10'},
  ];
  const categories=ref(seedCategories()), products=ref(seedProducts()), customers=ref(seedCustomers()), orders=ref(seedOrders());
  const currentCustomerId=ref(1), authMode=ref('register');
  const accountForm=ref({username:'试用客户',password:'demo12345',phone:'13900000003',defaultLocation:'图书馆入口'});
  const currentCustomer=computed(()=>customers.value.find(c=>c.id===currentCustomerId.value));
  const myOrders=computed(()=>orders.value.filter(o=>o.customerId===currentCustomerId.value));
  const categoryTree=computed(()=>categories.value.filter(c=>!c.parentId).map(c=>({value:c.id,label:c.name,children:categories.value.filter(x=>x.parentId===c.id).map(x=>({value:x.id,label:x.name}))})).map(c=>c.children.length?c:{value:c.value,label:c.label}));
  const categoryRows=computed(()=>categories.value.filter(c=>!c.parentId).flatMap(c=>[c,...categories.value.filter(x=>x.parentId===c.id)]));
  const plain=(html)=>{const doc=new DOMParser().parseFromString(String(html || ''),'text/html');return doc.body.textContent || '';};
  const money=(value)=>Number(value || 0).toFixed(2);
  const categoryName=(id)=>{const c=categories.value.find(c=>c.id===id);if(!c)return '未分类';const parent=categories.value.find(x=>x.id===c.parentId);return parent?parent.name+' / '+c.name:c.name;};
  const browseItems=computed(()=>products.value.filter(p=>{
   const word=search.value.trim().toLowerCase();
   const category=categories.value.find(c=>c.id===p.categoryId);
   return p.status==='在售'&&(!word||(p.name+' '+plain(p.description)).toLowerCase().includes(word))&&(!categoryFilter.value||p.categoryId===categoryFilter.value||category?.parentId===categoryFilter.value);
  }));
  const browsePage=computed(()=>browseItems.value.slice((page.value-1)*4,page.value*4));
  watch(()=>browseItems.value.length,total=>{page.value=Math.min(page.value,Math.max(1,Math.ceil(total/4)));});
  const filteredCustomers=computed(()=>{const word=customerSearch.value.trim().toLowerCase();return customers.value.filter(c=>!word||c.username.toLowerCase().includes(word)||c.phone.includes(word));});
  const detailVisible=ref(false), selectedProduct=ref(null), imageIndex=ref(0), orderLocation=ref('');
  const productFormVisible=ref(false), categoryFormVisible=ref(false), queueVisible=ref(false), customerHistoryVisible=ref(false), helpVisible=ref(false);
  const queueProduct=ref(null),selectedCustomer=ref(null),productForm=ref({}),categoryForm=ref({}),editor=ref(null),fileInput=ref(null),readingImages=ref(false);
  let nextProductId=108,nextCategoryId=7,nextCustomerId=3,nextOrderId=3006,imageReadVersion=0;
  function cleanHtml(html){
   const doc=new DOMParser().parseFromString(html || '','text/html');
   doc.querySelectorAll('script,style,iframe,object,embed').forEach(n=>n.remove());
   const allowed=new Set(['P','DIV','BR','B','STRONG','I','EM','U','H3','H2','UL','OL','LI','BLOCKQUOTE','IMG']);
   for(const node of [...doc.body.querySelectorAll('*')]){
    if(!allowed.has(node.tagName)){node.replaceWith(...node.childNodes);continue;}
    const src=node.getAttribute('src');
    for(const attr of [...node.attributes])node.removeAttribute(attr.name);
    if(node.tagName==='IMG'){
     if(src&&(/^(?:\.\/assets\/|data:image\/(?:jpeg|png);base64,)/i).test(src))node.setAttribute('src',src);
     else node.remove();
    }
   }
   return doc.body.innerHTML;
  }
  function showDetail(product){selectedProduct.value=product;imageIndex.value=0;orderLocation.value=currentCustomer.value?.defaultLocation || '';detailVisible.value=true;}
  function editProduct(product){
   imageReadVersion++;readingImages.value=false;
   productForm.value=product?{...product,images:[...product.images]}:{id:null,name:'',price:39,stock:2,categoryId:null,description:'<p>在这里填写商品介绍。</p>',images:[]};
   productFormVisible.value=true;
  }
  function mountEditor(){if(editor.value)editor.value.innerHTML=productForm.value.description || '';}
  function captureDescription(){if(editor.value)productForm.value.description=editor.value.innerHTML;}
  function format(command,value){if(!editor.value)return;editor.value.focus();document.execCommand(command,false,value);captureDescription();}
  function insertPhoto(){if(!productForm.value.images.length)return message.info('先选择一张相册图片');format('insertImage',productForm.value.images[0]);}
  async function addImages(event){
   if(readingImages.value)return;
   const files=Array.from(event.target.files || []);event.target.value='';
   const remaining=9-productForm.value.images.length;
   if(files.length>remaining)message.info('相册最多保留 9 张图片');
   const valid=files.slice(0,remaining).filter(file=>{
    if(!['image/png','image/jpeg'].includes(file.type)){message.warning(file.name+' 请选择 JPG 或 PNG');return false;}
    if(file.size>5*1024*1024){message.warning(file.name+' 超过 5 MB');return false;}
    return true;
   });
   const version=++imageReadVersion;readingImages.value=true;
   try{
    const urls=await Promise.all(valid.map(file=>new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=()=>reject(new Error('读取图片失败'));reader.readAsDataURL(file);} )));
    if(version===imageReadVersion&&productFormVisible.value)productForm.value.images.push(...urls);
   }catch{message.error('图片未读取成功，请重新选择');}
   finally{if(version===imageReadVersion)readingImages.value=false;}
  }
  function saveProduct(){
   if(readingImages.value)return;
   captureDescription();const form=productForm.value;
   if(!form.name?.trim())return message.warning('填写一个商品名称即可继续');
   if(!Number.isFinite(form.price)||form.price<=0||!Number.isInteger(form.stock)||form.stock<0)return message.warning('价格需大于 0，库存需为非负整数');
   const existing=products.value.find(p=>p.id===form.id);
   const value={id:form.id || nextProductId++,name:form.name.trim(),price:form.price,stock:form.stock,categoryId:form.categoryId || null,description:cleanHtml(form.description),images:[...form.images],status:form.stock>0?'在售':'下架',queue:existing?.queue || []};
   if(existing)Object.assign(existing,value);else products.value.unshift(value);
   productFormVisible.value=false;message.success(existing?'商品已更新；原购买快照保持不变':'商品已发布，可在商品浏览中查看');
  }
  function simulateSale(product){
   if(product.stock<=0||product.status!=='在售')return;
   product.stock--;if(product.stock===0){product.status='下架';product.queue=[];message.success(product.name+' 库存为 0，已自动下架');}
   else message.success(product.name+' 模拟成交 1 件，剩余库存 '+product.stock+'，其他商品不变');
   page.value=Math.max(1,Math.min(page.value,Math.ceil(browseItems.value.length/4)));
  }
  async function restock(product){
   try{const {value}=await ElementPlus.ElMessageBox.prompt('输入补货数量，确认后直接展示重新上架效果。','补货上架',{inputValue:'3',confirmButtonText:'补货上架',cancelButtonText:'取消',inputValidator:v=>Number.isInteger(Number(v))&&Number(v)>0 || '请输入正整数'});product.stock=Number(value);product.status='在售';message.success('已补货并重新上架');}catch{ /* 取消演示 */ }
  }
  function showQueue(product){queueProduct.value=product;queueVisible.value=true;}
  function editCategory(category){categoryForm.value=category?{...category}:{id:null,name:'',parentId:null};categoryFormVisible.value=true;}
  function saveCategory(){
   const form=categoryForm.value,name=form.name?.trim(),parentId=form.parentId || null;
   if(!name)return message.warning('请输入分类名称');
   if(form.id&&parentId&&categories.value.some(c=>c.parentId===form.id))return message.warning('包含子类的一级分类不能变成二级');
   if(categories.value.some(c=>c.id!==form.id&&c.name.toLowerCase()===name.toLowerCase()&&c.parentId===parentId))return message.warning('该层级已存在同名分类');
   const existing=categories.value.find(c=>c.id===form.id);
   if(existing)Object.assign(existing,{name,parentId});else categories.value.push({id:nextCategoryId++,name,parentId});
   categoryFormVisible.value=false;message.success('分类已保存');
  }
  function removeCategory(category){
   if(categories.value.some(c=>c.parentId===category.id)||products.value.some(p=>p.categoryId===category.id))return message.info('先调整子分类或商品关联，再删除该分类');
   categories.value=categories.value.filter(c=>c.id!==category.id);
   if(categoryFilter.value===category.id)categoryFilter.value=null;
   message.success('分类已删除');
  }
  function submitAccount(){
   const form=accountForm.value,user=form.username.trim();
   if(!user||!form.password)return message.warning('请输入演示用户名和密码');
   if(authMode.value==='login'){
    const customer=customers.value.find(c=>c.username.toLowerCase()===user.toLowerCase()&&c.password===form.password);
    if(!customer)return message.warning('演示账号或密码不匹配');
    currentCustomerId.value=customer.id;return message.success('已切换至 '+customer.username);
   }
   if(!form.phone.trim()||!form.defaultLocation.trim())return message.warning('请填写电话和默认交易地点');
   if(customers.value.some(c=>c.username.toLowerCase()===user.toLowerCase()))return message.warning('该用户名已存在，可切换到登录体验');
   const customer={id:nextCustomerId++,username:user,password:form.password,phone:form.phone.trim(),defaultLocation:form.defaultLocation.trim(),created:'2026-10-24'};
   customers.value.push(customer);currentCustomerId.value=customer.id;message.success('已创建本页临时客户，可去我的购买或卖家客户管理查看');
  }
  function simulateOrder(){
   if(!currentCustomer.value||!selectedProduct.value)return;
   if(!orderLocation.value.trim())return message.info('填写本次交易地点即可继续');
   const product=selectedProduct.value;
   orders.value.unshift({id:'D'+nextOrderId++,customerId:currentCustomerId.value,productName:product.name,price:product.price,location:orderLocation.value.trim(),status:'排队中',date:'2026-10-24 '+new Date().toLocaleTimeString('zh-CN',{hour12:false}).slice(0,5)});
   detailVisible.value=false;view.value='history';message.success('已添加一条本页购买记录，商品库存未扣减');
  }
  function resetDemo(){
   imageReadVersion++;readingImages.value=false;
   categories.value=seedCategories();products.value=seedProducts();customers.value=seedCustomers();orders.value=seedOrders();
   currentCustomerId.value=1;search.value='';categoryFilter.value=null;page.value=1;customerSearch.value='';view.value='browse';authMode.value='register';
   accountForm.value={username:'试用客户',password:'demo12345',phone:'13900000003',defaultLocation:'图书馆入口'};
   [detailVisible,productFormVisible,categoryFormVisible,queueVisible,customerHistoryVisible,helpVisible].forEach(r=>r.value=false);
   selectedProduct.value=null;queueProduct.value=null;selectedCustomer.value=null;
   nextProductId=108;nextCategoryId=7;nextCustomerId=3;nextOrderId=3006;message.success('演示数据已恢复');
  }
  return {view,search,categoryFilter,page,customerSearch,categories,products,customers,orders,currentCustomerId,authMode,accountForm,currentCustomer,myOrders,categoryTree,categoryRows,browseItems,browsePage,filteredCustomers,plain,money,categoryName,detailVisible,selectedProduct,imageIndex,orderLocation,productFormVisible,categoryFormVisible,queueVisible,customerHistoryVisible,helpVisible,queueProduct,selectedCustomer,productForm,categoryForm,editor,fileInput,readingImages,cleanHtml,showDetail,editProduct,mountEditor,captureDescription,format,insertPhoto,addImages,saveProduct,simulateSale,restock,showQueue,editCategory,saveCategory,removeCategory,submitAccount,simulateOrder,resetDemo};
 }
}).use(ElementPlus).mount('#app');
