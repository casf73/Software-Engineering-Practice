from pathlib import Path
import json, shutil
from PIL import Image
ROOT=Path(__file__).resolve().parent
ROUNDS={1:ROOT/'验收测试数据',2:ROOT/'第二轮验收测试数据',3:ROOT/'第三轮验收测试数据'}
METHODS={'等价类划分':'将输入分为合法和不合法类别，分别选择代表数据。','边界值分析':'检查长度、数量和金额边界及相邻值，例如库存-1、0、1，图片0、1、9、10张。','状态转换':'按在售、手动冻结、交易中、下架及意向状态检查允许和禁止的操作。','场景法':'按用户完整业务路径组织多步操作，并检查结果及历史记录。','权限隔离':'分别使用访客、客户甲、客户乙和卖家会话，检查数据归属及访问限制。','兼容性与恢复':'检查既有记录读取、页面尺寸和服务重启后的保存状态。'}
CASES={1:{'T01':'卖家登录与权限','T02':'商品发布和编辑','T03':'口令码与 FIFO 队列','T04':'交易冻结及撤销边界','T05':'交易失败并作废','T06':'交易失败重新排队','T07':'单件商品成交收尾','T08':'卖家主动下架','T09':'图片上传和文件限制','T10':'既有图片记录读取','U01':'商品页面浏览','U02':'意向表单与查询','U03':'页面队列及开始交易','U04':'页面冻结与撤销','U05':'服务重启与数据保留','U06':'上传页面与无图发布','U07':'桌面及窄屏显示'},2:{'R201':'等待买家修改联系信息','R202':'手动冻结与解冻','R203':'卖家改密与会话撤销'},3:{'R301':'多商品发布与独立队列','R302':'两级分类管理','R303':'库存扣减与售罄下架','R304':'浏览搜索与分类分页','R305':'多图与富媒体描述','R306':'客户注册登录与默认地点','R307':'客户购买历史及隔离','R308':'卖家客户信息与购买历史'}}
def write(p,obj):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
class Scenario:
 def __init__(self,r,case,n,title,method='场景法'):
  self.folder=ROUNDS[r]/'接口测试'/case/f'第{n}个测试实例';self.folder.mkdir(parents=True,exist_ok=True)
  self.meta={'id':f'{case}-{n:02d}','caseId':case,'iteration':r,'type':'接口测试','title':title,'mode':'automated','designMethod':method,'preconditions':'独立内存 H2；每个实例前清理测试数据，卖家初始 admin/123456。','steps':[],'expected':'逐步验证 HTTP 状态码及响应字段，详见步骤断言。','result':'待执行'}
 def step(self,method,path,status=200,body=None,expect=None,capture=None,session='seller',save=None,file=None,mime=None,absent=None):
  row={'method':method,'path':path,'status':status}
  if session:row['session']=session
  if save:row['saveSession']=save
  if body is not None:
   name=f'请求数据{len(self.meta["steps"])+1:02d}.json';write(self.folder/name,body);row['bodyFile']=name
  if expect:row['assertions']=expect
  if capture:row['capture']=capture
  if absent:row['absent']=absent
  if file:row.update(file=file,mime=mime)
  self.meta['steps'].append(row)
 def login(self,password='123456',key='seller'):self.step('POST','/api/seller/login',body={'username':'admin','password':password},session=None,save=key,expect={'/authenticated':True})
 def product(self,name='测试商品',stock=1,key='p',category=None,images=None,description='商品说明',price=88.5):
  self.step('POST','/api/seller/products',201,{'name':name,'price':price,'stock':stock,'categoryId':category,'images':images or [],'description':description},capture={key:'/id'})
 def submit(self,p='p',key='code',name='买家甲',phone='13800000001',position=1):
  self.step('POST',f'/api/public/products/${{{p}}}/intents',201,{'name':name,'phone':phone},session=None,capture={key:'/passcode'},expect={'/position':position})
 def category(self,name='电子产品',parent=None,key='cat'):self.step('POST','/api/seller/categories',201,{'name':name,'parentId':parent},capture={key:'/id'})
 def start(self,p='p',key='trade'):self.step('POST',f'/api/seller/products/${{{p}}}/trades/start',capture={key:'/id'},expect={'/status':'IN_TRADE'})
 def finish(self,success=True,disposition=None):self.step('POST','/api/seller/trades/${trade}/result',204,{'success':success,'disposition':disposition})
 def register(self,username='buyer01',password='Buyer1234',phone='13800000001',location='学校南门',key='buyer'):
  self.step('POST','/api/customer/register',201,{'username':username,'password':password,'phone':phone,'defaultLocation':location},session=None,save=key,capture={key+'Id':'/customer/id'},expect={'/authenticated':True,'/customer/username':username,'/customer/defaultLocation':location},absent=['/customer/passwordHash','/customer/password'])
 def buy(self,key='buyer',p='p',code='code',location='学校南门'):
  self.step('POST',f'/api/customer/products/${{{p}}}/intents',201,{'name':'客户联系人','phone':'13900000001','meetingLocation':location},session=key,capture={code:'/passcode'})
 def image(self,name='sample.png',fmt='PNG',size=0):
  if size:
   with (self.folder/name).open('wb') as out:out.write(b'\x89PNG\r\n\x1a\n');out.truncate(size)
  else:Image.new('RGB',(16,16),(60,110,160)).save(self.folder/name,fmt)
 def upload(self,name='sample.png',key='image',mime='image/png',status=200):self.step('UPLOAD','/api/seller/products/image',status,file=name,mime=mime,capture={key:'/url'} if status==200 else None)
 def save(self):write(self.folder/'instance.json',self.meta)
def migrate():
 # 只在指定测试目录内移动；目标已存在时拒绝覆盖。
 for r in (1,2):
  for d in list(ROUNDS[r].iterdir()):
   if not d.is_dir() or d.name in ['功能测试','接口测试']:continue
   if r==1 and (d.name.startswith('T') or d.name.startswith('U')):target=ROUNDS[r]/('接口测试' if d.name.startswith('T') else '功能测试')/d.name
   elif r==2 and d.name.startswith('R20'):
    i=int(d.name[1:]);target=(ROUNDS[2]/'接口测试'/d.name) if i<=203 else (ROUNDS[3]/'接口测试'/f'R{301+i-204}')
   else:continue
   if target.exists():raise RuntimeError(f'目标已存在，拒绝覆盖 {target}')
   target.parent.mkdir(parents=True,exist_ok=True)
   assert d.resolve().is_relative_to(ROOT.resolve()) and target.resolve().is_relative_to(ROOT.resolve())
   shutil.move(str(d),str(target))
 for r,folder in ROUNDS.items():
  for p in folder.rglob('instance.json'):
   obj=json.loads(p.read_text(encoding='utf-8-sig'));case=p.parent.parent.name;n=int(p.parent.name[1:p.parent.name.index('个')])
   obj.update(id=f'{case}-{n:02d}',caseId=case,iteration=r,type=p.parent.parent.parent.name)
   obj.setdefault('designMethod','场景法');obj['result']='待执行';obj.pop('actual',None)
   if case=='R304' and obj['type']=='接口测试':obj['steps']=[x for x in obj['steps'] if not x['path'].startswith(('/api/customer','/api/seller/customers'))]
   write(p,obj)
def first_additional():
 for case in [f'T{i:02}' for i in range(1,11)]:
  for n in range(4,9):
   s=Scenario(1,case,n,'', '等价类划分')
   if case=='T01':
    name,pwd=[('admin',''),('missing','123456'),('admin','123456 '),('admin','WRONG'),('admin','123456')][n-4]
    s.meta['title']=['密码为空','未知账号','密码尾随空格','错误密码','登录后退出权限'][n-4]
    s.step('GET','/api/seller/workbench',401,session=None)
    s.step('POST','/api/seller/login',200 if n==8 else (400 if n==4 else 401),{'username':name,'password':pwd},session=None,save='seller' if n==8 else None)
    if n==8:s.step('POST','/api/seller/logout',204);s.step('GET','/api/seller/workbench',401)
   elif case=='T02':
    s.login();price=[0,0.01,-0.01,99.999,9999999999.99][n-4];s.meta.update(title=f'价格边界 {price}',designMethod='边界值分析')
    s.step('POST','/api/seller/products',201 if n in (5,8) else 400,{'name':'价格测试','price':price,'images':[]})
   elif case in ('T03','T04','T05','T06','T07','T08'):
    s.login();s.product();s.submit();s.submit(key='second',name='买家乙',phone='13800000002',position=2)
    s.meta['designMethod']='状态转换'
    if case=='T03':
     s.meta['title']=['无效口令码','撤销后口令失效','队首撤销排位前移','作废队首','队首重新排队'][n-4]
     if n==4:s.step('GET','/api/public/intents/INVALID00',404,session=None)
     elif n in (5,6):s.step('DELETE','/api/public/intents/${code}',204,session=None);s.step('GET','/api/public/intents/${code}',404,session=None);s.step('GET','/api/public/intents/${second}',session=None,expect={'/position':1})
     else:
      s.step('GET','/api/seller/products/${p}/workbench',capture={'intent':'/intents/0/id'})
      s.step('POST','/api/seller/intents/${intent}/action',204,{'action':'VOID' if n==7 else 'REQUEUE'})
      s.step('GET','/api/public/intents/${second}',session=None,expect={'/position':1})
    elif case=='T04':
     s.start();s.meta['title']=['交易中拒绝新意向','交易者禁止撤销','等待者可撤销','交易中禁止下架','重复开始交易拒绝'][n-4]
     if n==4:s.step('POST','/api/public/products/${p}/intents',409,{'name':'丙','phone':'13800000003'},session=None)
     elif n in (5,6):s.step('DELETE','/api/public/intents/${'+('code' if n==5 else 'second')+'}',409 if n==5 else 204,session=None)
     elif n==7:s.step('POST','/api/seller/products/${p}/delist',409)
     else:s.step('POST','/api/seller/products/${p}/trades/start',409)
    elif case in ('T05','T06','T07'):
     s.meta['title']=( '失败作废' if case=='T05' else '失败重排' if case=='T06' else '单件成交')+['后的商品状态','后的口令状态','后的等待排位','后的卖家队列','后的重复登记限制'][n-4]
     s.start();s.finish(case=='T07','VOID' if case=='T05' else 'REQUEUE' if case=='T06' else None)
     if n==8:s.step('POST','/api/seller/trades/${trade}/result',409,{'success':True})
     elif case=='T07':s.step('GET','/api/public/products/${p}',404,session=None);s.step('GET','/api/public/intents/${second}',404,session=None)
     else:
      s.step('GET','/api/public/products/${p}',session=None,expect={'/status':'ON_SALE'})
      s.step('GET','/api/public/intents/${code}',404 if case=='T05' else 200,session=None,expect={'/position':2} if case=='T06' else None)
      s.step('GET','/api/public/intents/${second}',session=None,expect={'/position':1})
    else:
     s.meta['title']=['下架停止公开展示','下架使口令失效','下架结束等待记录','下架后重复下架拒绝','下架后可发下一商品'][n-4]
     s.step('POST','/api/seller/products/${p}/delist',204)
     if n==7:s.step('POST','/api/seller/products/${p}/delist',409)
     elif n==8:s.product('下一商品',key='next')
     else:s.step('GET','/api/public/products/${p}',404,session=None);s.step('GET','/api/public/intents/${code}',404,session=None)
   elif case=='T09':
    s.login();s.meta.update(title=['PNG 正常上传','JPG 正常上传','错误 MIME 类型','5 MB 临界值','5 MB 加 1 字节'][n-4],designMethod='边界值分析')
    if n==5:s.image('sample.jpg','JPEG');s.upload('sample.jpg',mime='image/jpeg')
    elif n==6:(s.folder/'invalid.txt').write_text('not image',encoding='utf-8');s.upload('invalid.txt',mime='text/plain',status=400)
    else:s.image(size=5*1024*1024+(1 if n==8 else 0) if n>=7 else 0);s.upload(status=400 if n==8 else 200)
   else:
    s.login();s.meta['title']=['空图片列表','单图 JPG 读取','单图 PNG 读取','重命名不丢图片','图片地址顺序保持'][n-4]
    images=[] if n==4 else ['/uploads/existing.jpg' if n%2 else '/uploads/existing.png']
    s.product(images=images);s.step('GET','/api/public/products/${p}',session=None,expect={'/images':images})
    if n==7:s.step('PUT','/api/seller/products/${p}',body={'name':'更新名称','price':88.5,'images':images},expect={'/images':images})
   s.save()
def second():
 for n in range(1,9):
  s=Scenario(2,'R201',n,['正常修改姓名与电话','手动冻结中修改','他人交易期间修改','姓名为空','电话非法','姓名80字边界','姓名81字越界','已撤销意向禁止修改'][n-1],'边界值分析' if n>=4 else '状态转换')
  s.login();s.product(stock=1);s.submit();s.submit(key='second',name='等待买家',phone='13800000002',position=2)
  if n==2:s.step('POST','/api/seller/products/${p}/freeze')
  if n==3:s.start();s.step('PUT','/api/public/intents/${code}',409,{'name':'交易者','phone':'13900000001'},session=None)
  if n==8:s.step('DELETE','/api/public/intents/${second}',204,session=None)
  name=' ' if n==4 else '名'*80 if n==6 else '名'*81 if n==7 else '更新姓名'+str(n)
  phone='phone-bad' if n==5 else '1390000000'+str(n)
  status=400 if n in (4,5,7) else 404 if n==8 else 200
  s.step('PUT','/api/public/intents/${second}',status,{'name':name,'phone':phone},session=None,expect={'/name':name,'/phone':phone,'/position':1 if n==3 else 2} if status==200 else None)
  if status==200:s.step('GET','/api/public/intents/${second}',session=None,expect={'/name':name,'/position':1 if n==3 else 2})
  s.save()
  s=Scenario(2,'R202',n,['冻结保留队列','冻结拒绝新意向','解冻恢复在售','解冻后队首交易','交易冻结禁止手动解除','重复冻结拒绝','在售重复解冻拒绝','访客无权冻结'][n-1],'状态转换')
  s.login();s.product();s.submit()
  if n==8:s.step('POST','/api/seller/products/${p}/freeze',401,session=None)
  elif n==7:s.step('POST','/api/seller/products/${p}/unfreeze',409)
  elif n==5:s.start();s.step('POST','/api/seller/products/${p}/unfreeze',409)
  else:
   s.step('POST','/api/seller/products/${p}/freeze',expect={'/status':'MANUALLY_FROZEN'})
   if n==1:s.step('GET','/api/public/intents/${code}',session=None,expect={'/position':1})
   elif n==2:s.step('POST','/api/public/products/${p}/intents',409,{'name':'新买家','phone':'13800000002'},session=None)
   elif n==6:s.step('POST','/api/seller/products/${p}/freeze',409)
   else:s.step('POST','/api/seller/products/${p}/unfreeze',expect={'/status':'ON_SALE'});s.start() if n==4 else None
  s.save()
  s=Scenario(2,'R203',n,['原密码错误','新密码7位下界外','新密码8位下界','新密码128位上界','新密码129位越界','改密撤销另一个旧会话','旧密码不能再次登录','未登录不能改密'][n-1],'边界值分析' if n in (2,3,4,5) else '权限隔离')
  s.login();s.login(key='other')
  new='a'*7 if n==2 else 'B'*8 if n==3 else 'C'*128 if n==4 else 'D'*129 if n==5 else 'new-password-2026'
  status=400 if n in (1,2,5) else 401 if n==8 else 204
  s.step('PUT','/api/seller/password',status,{'currentPassword':'wrong' if n==1 else '123456','newPassword':new},session=None if n==8 else 'seller')
  if status==204:
   s.step('GET','/api/seller/workbench',401,session='other')
   s.step('POST','/api/seller/login',401,{'username':'admin','password':'123456'},session=None)
   s.login(new,key='newSeller')
  else:s.step('GET','/api/seller/session',session='other',expect={'/authenticated':True})
  s.save()
def third_a1():
 for n in range(4,9):
  s=Scenario(3,'R301',n,['不同商品可同时接单','冻结甲不影响乙','甲成交不影响乙排位','下架甲不影响乙','多商品旧current接口冲突'][n-4],'场景法')
  s.login();s.product('商品甲');s.product('商品乙',key='other');s.submit();s.submit(p='other',key='otherCode')
  if n==4:s.step('GET','/api/public/products',session=None,expect={'/total':2})
  elif n==5:s.step('POST','/api/seller/products/${p}/freeze');s.submit(p='other',key='third',position=2)
  elif n==6:s.start();s.finish();s.step('GET','/api/public/intents/${otherCode}',session=None,expect={'/position':1})
  elif n==7:s.step('POST','/api/seller/products/${p}/delist',204);s.step('GET','/api/public/products/${other}',session=None,expect={'/status':'ON_SALE'})
  else:s.step('GET','/api/public/products/current',409,session=None)
  s.save()
  s=Scenario(3,'R302',n,['同级重名拒绝','不同父类同名允许','禁止三级分类','禁止父子循环','被商品引用不可删除'][n-4],'等价类划分')
  s.login();s.category('电子产品');s.category('键盘',parent='${cat}',key='child')
  if n==4:s.step('POST','/api/seller/categories',409,{'name':'键盘','parentId':'${cat}'})
  elif n==5:s.category('办公',key='other');s.category('键盘',parent='${other}',key='allowed')
  elif n==6:s.step('POST','/api/seller/categories',400,{'name':'三级','parentId':'${child}'})
  elif n==7:s.step('PUT','/api/seller/categories/${cat}',400,{'name':'电子产品','parentId':'${child}'})
  else:s.product(category='${child}');s.step('DELETE','/api/seller/categories/${child}',409)
  s.save()
  s=Scenario(3,'R303',n,['零库存发布即下架','负库存拒绝','库存1000000上界','库存1000001越界','最后一件成交自动下架'][n-4],'边界值分析')
  s.login()
  if n in (5,7):s.step('POST','/api/seller/products',400,{'name':'越界库存','price':88.5,'stock':-1 if n==5 else 1000001})
  else:
   s.product(stock=0 if n==4 else 1000000 if n==6 else 1)
   if n==4:s.step('GET','/api/public/products/${p}',404,session=None)
   elif n==6:s.step('GET','/api/public/products/${p}',session=None,expect={'/stock':1000000})
   else:s.submit();s.start();s.finish();s.step('GET','/api/seller/products/${p}/workbench',expect={'/product/stock':0,'/product/status':'DELISTED'})
  s.save()
  s=Scenario(3,'R304',n,['名称关键字大小写','富文本可见文字搜索','一级分类含二级','无匹配关键字','分页参数边界'][n-4],'等价类划分' if n<8 else '边界值分析')
  s.login();s.category();s.category('键盘',parent='${cat}',key='child');s.product('Keyboard键盘',category='${child}',description='<p>机械键盘 &amp; 办公</p>')
  if n==4:s.step('GET','/api/public/products?q=keyboard',session=None,expect={'/total':1})
  elif n==5:s.step('GET','/api/public/products?q=%E5%8A%9E%E5%85%AC',session=None,expect={'/total':1})
  elif n==6:s.step('GET','/api/public/products?categoryId=${cat}',session=None,expect={'/total':1})
  elif n==7:s.step('GET','/api/public/products?q=nomatch',session=None,expect={'/total':0,'/items':[]})
  else:
   for query in ['page=-1','size=0','size=101']:s.step('GET','/api/public/products?'+query,400,session=None)
   s.step('GET','/api/public/products?size=1&page=1',session=None,expect={'/total':1,'/items':[]})
  s.save()
  s=Scenario(3,'R305',n,['无图发布','9张图片上界','10张图片越界','非法富媒体图片地址','富文本编辑二次保存'][n-4],'边界值分析' if n<=6 else '等价类划分')
  s.login();s.image();s.upload()
  if n==4:s.product(images=[]);s.step('GET','/api/public/products/${p}',session=None,expect={'/images':[]})
  elif n in (5,6):
   body={'name':'多图','price':88.5,'images':['${image}']*(9 if n==5 else 10)}
   s.step('POST','/api/seller/products',201 if n==5 else 400,body)
  elif n==7:s.step('POST','/api/seller/products',400,{'name':'非法图片','price':88.5,'images':['javascript:alert(1)']})
  else:
   desc='<h3>介绍</h3><p><strong>九成新</strong></p>';s.product(description=desc)
   s.step('PUT','/api/seller/products/${p}',body={'name':'再次保存','price':88.5,'description':desc,'images':[]},expect={'/description':desc})
  s.save()
def third_a2():
 for n in range(1,9):
  s=Scenario(3,'R306',n,['合法注册和默认地点','用户名2位过短','用户名3位下界','用户名32位上界','用户名33位过长','密码7位不足','同名不同大小写冲突','登录退出会话'][n-1],'边界值分析' if n in (2,3,4,5,6) else '等价类划分')
  if n in (1,3,4,7,8):
   user='abc' if n==3 else 'u'*32 if n==4 else 'buyer01';s.register(user)
   if n==1:s.step('GET','/api/customer/session',session='buyer',expect={'/customer/phone':'13800000001','/customer/defaultLocation':'学校南门'})
   elif n==7:s.step('POST','/api/customer/register',409,{'username':'BUYER01','password':'Buyer1234','phone':'13900000001','defaultLocation':'东门'},session=None)
   elif n==8:
    s.step('POST','/api/customer/logout',204,session='buyer');s.step('GET','/api/customer/orders',401,session='buyer')
    s.step('POST','/api/customer/login',401,{'username':user,'password':'wrong'},session=None)
    s.step('POST','/api/customer/login',body={'username':user,'password':'Buyer1234'},session=None,save='again',expect={'/authenticated':True})
  else:s.step('POST','/api/customer/register',400,{'username':'ab' if n==2 else 'x'*33 if n==5 else 'buyer01','password':'a'*7 if n==6 else 'Buyer1234','phone':'13800000001','defaultLocation':'学校南门'},session=None)
  s.save()
  s=Scenario(3,'R307',n,['下单保存商品价格与地点','客户间历史隔离','访客意向不自动认领','撤销历史仍保留','成功交易历史仍保留','商品改名不改变快照','未登录历史请求拒绝','历史分页边界'][n-1],'权限隔离' if n in (2,3,7) else '场景法')
  s.login();s.product();s.register();s.buy()
  if n==1:s.step('GET','/api/customer/orders',session='buyer',expect={'/total':1,'/items/0/productName':'测试商品','/items/0/price':88.5,'/items/0/meetingLocation':'学校南门','/items/0/status':'WAITING'})
  elif n==2:s.register('buyer02',key='otherBuyer');s.step('GET','/api/customer/orders',session='otherBuyer',expect={'/total':0});s.step('GET','/api/customer/orders',session='buyer',expect={'/total':1})
  elif n==3:s.submit(key='guest',name='客户联系人',phone='13900000001',position=2);s.step('GET','/api/customer/orders',session='buyer',expect={'/total':1})
  elif n==4:s.step('DELETE','/api/public/intents/${code}',204,session=None);s.step('GET','/api/customer/orders',session='buyer',expect={'/items/0/status':'CANCELLED','/items/0/passcode':None})
  elif n==5:s.start();s.finish();s.step('GET','/api/customer/orders',session='buyer',expect={'/items/0/status':'TRADE_SUCCESS','/items/0/passcode':None})
  elif n==6:s.step('PUT','/api/seller/products/${p}',body={'name':'新名称','price':999,'stock':1,'images':[]});s.step('GET','/api/customer/orders',session='buyer',expect={'/items/0/productName':'测试商品','/items/0/price':88.5})
  elif n==7:s.step('GET','/api/customer/orders',401,session=None);s.step('POST','/api/customer/products/${p}/intents',401,{'name':'甲','phone':'13800000001','meetingLocation':'东门'},session=None)
  else:
   s.step('GET','/api/customer/orders?page=1&size=1',session='buyer',expect={'/total':1,'/items':[]})
   s.step('GET','/api/customer/orders?size=0',400,session='buyer')
  s.save()
  s=Scenario(3,'R308',n,['卖家查看客户基本信息','用户名搜索','电话搜索','客户历史及隐私字段','禁止访客查看客户','禁止客户越权卖家接口','不存在客户返回404','客户列表分页边界'][n-1],'权限隔离' if n in (4,5,6) else '等价类划分')
  s.login();s.product();s.register();s.buy()
  if n==1:s.step('GET','/api/seller/customers/${buyerId}',expect={'/username':'buyer01','/phone':'13800000001','/defaultLocation':'学校南门'},absent=['/passwordHash','/password'])
  elif n in (2,3):s.step('GET','/api/seller/customers?q='+('buyer' if n==2 else '13800000001'),expect={'/total':1,'/items/0/username':'buyer01'})
  elif n==4:s.step('GET','/api/seller/customers/${buyerId}/orders',expect={'/total':1,'/items/0/productName':'测试商品','/items/0/passcode':None})
  elif n in (5,6):s.step('GET','/api/seller/customers',401,session=None if n==5 else 'buyer');s.step('GET','/api/seller/customers/${buyerId}/orders',401,session=None if n==5 else 'buyer')
  elif n==7:s.step('GET','/api/seller/customers/999999999',404);s.step('GET','/api/seller/customers/999999999/orders',404)
  else:s.step('GET','/api/seller/customers?size=0',400);s.step('GET','/api/seller/customers?page=1&size=1',expect={'/items':[],'/total':1})
  s.save()
MANUAL_FIRST={
'U01':[('无商品页面','不发布商品，打开首页','显示空状态'),('无图商品','商品名=课程书，价格=20，不选图片','商品文字及无图占位正常'),('单图商品','上传sample.png并发布课程书','商品图片与名称正常'),('价格显示','发布价格=0.01的商品','价格显示0.01元'),('冻结商品展示','进入交易后刷新首页','仍显示商品并提示交易中'),('下架商品','卖家下架后刷新首页','商品不再公开展示'),('刷新页面','发布课程书后按F5','加载最新商品数据'),('匿名浏览','清除登录状态后打开首页','无需登录即可浏览')],
'U02':[('姓名为空','电话13800000001，姓名留空','提示姓名必填'),('电话为空','姓名买家甲，电话留空','提示电话必填'),('正常提交查询','姓名买家甲，电话13800000001，提交并保存口令','显示有效排位'),('非法电话','姓名买家甲，电话phone-bad','提示电话格式错误'),('无效口令','输入INVALID00查询','提示口令无效'),('首尾空格口令','有效口令前后各加一个空格查询','仍可查询本人意向'),('小写口令','将有效口令转为小写查询','仍可查询本人意向'),('撤销后再查询','提交、撤销后使用原口令查询','提示失效')],
'U03':[('两人顺序','甲13800000001、乙13800000002依次提交','甲先乙后'),('三人队首交易','甲乙丙依次提交，卖家开始交易','只有甲交易中'),('撤销后位置','甲乙丙依次提交，乙撤销','丙升为第2位'),('作废后位置','卖家作废队首甲','乙升为队首'),('重新排队','卖家将甲移到队尾','乙丙甲顺序'),('刷新队列','乙新提交意向后点击刷新','队列读取最新数据'),('空队列开始交易','无买家时点击开始交易','不生成交易'),('重复开始交易','已有交易时再次尝试开始','不能重复创建交易')],
'U04':[('交易者撤销','甲乙依次提交并开始甲交易，甲尝试撤销','甲不能撤销'),('等待者撤销','甲交易期间乙撤销','乙退出，甲交易保留'),('冻结拒绝新意向','甲交易期间丙尝试提交','不接收新意向'),('失败作废','甲交易失败选择作废','甲口令失效，乙保留'),('失败重新排队','甲交易失败选择重新排队','甲移到队尾，原码有效'),('成功收尾','库存1，甲成交，乙等待','售罄下架，乙结束'),('主动下架','在售期间卖家下架','有效等待口令失效'),('冻结期查询','甲交易期间乙凭口令查询','仍能看到等待状态')],
'U05':[('商品保存','发布课程书20元，停止后端后在原工作目录启动','商品保留'),('意向保存','提交甲13800000001并保存口令，再重启','意向保留'),('顺序保存','甲乙依次提交后重启','顺序不变'),('图片保存','上传sample.png后重启','图片可加载'),('下架保存','下架商品后重启','不重新上架'),('成功结果保存','库存1成交后重启','成功记录保留'),('撤销结果保存','撤销意向后重启','原口令仍失效'),('失败重排保存','甲失败重排后重启','甲仍在队尾')],
'U06':[('无图发布','填写课程书和20元，图片留空','允许发布'),('单图发布','选择sample.png后保存','显示图片'),('替换图片','选择sample.jpg替换当前图片后保存','显示替换结果'),('错误格式','选择invalid.txt','提示格式错误'),('超大图片','选择5MB加1字节图片','提示超过5MB'),('取消文件选择','打开选择文件窗口后取消','表单内容不丢失'),('删除预览','选图后移除再保存','按无图商品发布'),('JPG上传','选择sample.jpg','允许上传并展示')],
'U07':[('桌面买家页','视口1440×900打开首页','内容可读可操作'),('窄屏买家页','视口375×812打开首页','可查看商品与表单'),('窄屏卖家页','视口375×812登录后台','菜单与表单可操作'),('桌面弹窗','1440×900打开口令查询','输入与按钮可见'),('窄屏弹窗','375×812打开查询弹窗','关键输入与按钮可操作'),('长名称','商品名80字，1440×900查看','文字换行不遮挡按钮'),('长描述','商品描述多段文字，375×812查看','正文可滚动阅读'),('尺寸切换','首页从1440切至375再恢复','布局可恢复')]
}
UI={
'R201':'在首页口令码查询弹窗中查询对应等待买家，点击修改联系信息，填写本实例姓名、电话后保存，再查询排位。',
'R202':'卖家登录商品管理，选定测试商品，按实例场景点击手动冻结或解除冻结，再在买家页面尝试提交并查看原排位。',
'R203':'卖家登录账号管理，输入本实例原密码、新密码及一致的确认密码，点击保存；在另一浏览器窗口验证旧会话，再尝试新旧密码登录。',
'R301':'卖家在商品管理逐件发布甲、乙；买家切换商品分别提交意向。对甲执行实例中的冻结、成交或下架操作，刷新乙的队列。',
'R302':'卖家进入分类管理，新建一级电子产品和二级键盘，按实例执行新建、编辑、删除，再在商品表单选择分类。',
'R303':'卖家在商品管理输入本实例库存并保存；需要成交时，买家先提交，卖家开始交易并登记成功，再刷新库存和公开列表。',
'R304':'首页搜索栏输入本实例关键字，切换一级/二级分类并翻页，再点击重置，核对名称、描述和列表数量。',
'R305':'卖家编辑商品，选择实例图片，编辑标题、粗体、列表及描述图片后保存；买家切换缩略图，卖家重新打开并保存。',
'R306':'打开客户注册页，输入本实例用户名、密码、确认密码、电话和默认交易地点并提交；需要时退出后在客户登录页重新登录。',
'R307':'客户登录后在首页提交购买意向，确认联系信息和交易地点，再进入我的购买；按实例切换另一客户、撤销意向或完成交易后刷新历史。',
'R308':'卖家登录后进入客户管理，按用户名或电话搜索客户，打开购买历史，核对联系人、默认地点、商品与交易状态。'
}
FUNCTION_EXPECTATIONS={'R201': ['修改后姓名电话更新，原口令与第2位排位不变。', '手动冻结时等待者可保存新姓名电话，排位仍第2。', '队首交易者不能改；其他等待者可改，等待排位为第1。', '空白姓名提示必填，不保存空值，原联系信息保留。', '非法电话phone-bad提示格式不正确，原联系信息保留。', '80字姓名保存成功，查询仍返回原排位。', '81字姓名被输入长度限制或保存校验阻止，原数据不变。', '撤销后原码失效，再查询或修改提示无效。'], 'R202': ['冻结后仍能查询原口令和第1位，等待队列不丢失。', '冻结商品不能提交新意向，已有队列保留。', '手动解冻恢复在售，可重新接收意向。', '解冻后点击开始交易，只选原队首。', '交易冻结时不能点击手动解冻绕过交易结果。', '重复冻结提示状态不允许，不清空队列。', '在售商品重复解冻不产生新状态或交易。', '未登录卖家时不能冻结商品，页面要求登录。'], 'R203': ['原密码wrong被拒绝，旧登录和原密码仍有效。', '7位新密码被阻止保存，原密码不变。', '8位新密码保存成功，需使用新密码登录。', '128位新密码保存成功，旧会话失效。', '129位输入被长度限制或校验阻止，不修改原密码。', '一处改密后另一卖家窗口访问后台要求重新登录。', '改密后旧密码123456无法登录，新密码可以登录。', '未登录时不能访问改密操作，不保存任何密码。'], 'R301': ['两个商品均可见，各自第一个买家排第1位；甲交易不改变乙状态。', '三个商品都可独立排队，各自队首和库存正确。', '四个商品都保留自身队列，操作一件不影响其余。', '甲乙均可接收意向，各自显示自己的买家和排位。', '冻结甲后乙仍可提交新意向，乙队列递增。', '甲成交后乙仍在售，乙原买家仍是第1位。', '甲下架后乙仍公开展示并保留原队列。', '首页及后台以指定商品操作，不串到另一个商品。'], 'R302': ['商品关联二级键盘后，筛选其一级电子产品仍可看到。', '三级、循环和同级重名被拒绝，原分类层级不变。', '分类改名后同步显示；未被引用的分类允许删除。', '同一父类下重名键盘被拒绝。', '不同一级父类下可分别建立键盘二级分类。', '二级分类下不可建立第三级。', '不能把父分类设置为自己的子分类。', '被商品引用的分类不能删除，需先移除引用。'], 'R303': ['库存2首次成交剩1且在售，第二次成交剩0并下架。', '失败重排库存不变，重复登记结果不能再次扣库存。', '库存0不展示；补货后可上架，非法库存不保存。', '零库存商品保存为下架，买家首页不展示。', '库存-1被拒绝，商品数据不被错误保存。', '库存1000000允许保存并正确展示。', '库存1000001被拒绝，原库存不变。', '最后一件成功后库存0，下架并结束该商品剩余等待。'], 'R304': ['名称关键字匹配商品，下架商品不出现在公开结果。', '描述中的匹配关键字忽略大小写，结果正确。', '筛选一级包含二级商品，翻页无重复遗漏。', 'keyboard能匹配名称Keyboard键盘。', '办公能匹配富文本可见文字，不需要输入HTML标签。', '电子产品筛选包含其二级键盘商品。', 'nomatch显示空结果，重置后恢复列表。', '首页、末页和无结果页显示正确，禁用不允许的翻页。'], 'R305': ['两张图片按顺序展示，可切换，格式化描述保留。', '9张可保存；第10张、TXT和超大图片被提示限制。', '描述图片二次保存仍显示，危险内容不执行。', '不选图片允许发布，买家显示无图占位。', '9张相册全部保存且可切换缩略图。', '第10张图片被选择数量限制或保存校验阻止。', '编辑描述正常展示，脚本和非法图片不能执行加载。', '再次打开编辑并保存后，标题及粗体等格式不丢失。'], 'R306': ['注册后自动登录，电话和默认地点正确；空地点和非法电话被阻止。', '2位用户名被提示长度不足，不建立账号。', '3位用户名允许注册并登录。', '32位用户名允许注册并完整展示。', '33位用户名被长度限制或校验阻止，不建立超长账号。', '7位密码不能保存；8及128位可注册，129位被阻止。', 'BUYER01与buyer01视为重复用户名，不能重复注册。', '退出后不能查看购买历史；错误密码拒绝，正确密码可登录。'], 'R307': ['我的购买显示测试商品、88.50元和本次学校南门；空或超长地点不能下单。', '客户乙看不到客户甲的购买记录，甲记录保持。', '同姓名电话的访客意向不会自动加入客户购买历史。', '撤销后记录仍在，显示已撤销，不能再用原码操作。', '成交后历史仍在，显示交易成功，原口令不再可用。', '商品改名改价后历史仍显示下单时测试商品和88.50元。', '未登录访问我的购买要求登录，不能通过隐藏按钮下单。', '购买历史首页末页及空页正确，不重复、不串号。'], 'R308': ['卖家看到用户名、电话、默认地点、注册时间，不看到密码内容。', '按buyer可搜到buyer01，其他不匹配客户不显示。', '按13800000001搜到对应客户，联系方式正确。', '指定客户历史商品、状态、地点正确，页面不展示客户口令。', '访客不能访问客户管理和他人历史。', '仅客户登录不能进入卖家客户管理。', '搜索nomatch显示暂无客户，重置后恢复列表。', '客户列表分页正确；同浏览器退出卖家身份后客户登录仍有效。']}

def functional():
 for case,rows in MANUAL_FIRST.items():
  for n,(title,action,expected) in enumerate(rows,1):
   folder=ROUNDS[1]/'功能测试'/case/f'第{n}个测试实例';folder.mkdir(parents=True,exist_ok=True)
   data={'商品名':'课程书','价格':20,'姓名':'买家甲','电话':'13800000001','操作数据':action}
   meta={'id':f'{case}-{n:02d}','caseId':case,'iteration':1,'type':'功能测试','mode':'manual','title':title,'preconditions':'仅验收首轮商品、口令码、队列和交易基础功能。使用隔离的手工测试数据目录。','designMethod':'兼容性与恢复' if case in ('U05','U07') else '场景法','steps':['准备本实例测试数据。',action,'记录页面实际结果并保留截图。'],'expected':expected,'result':'待执行','dataFile':'测试数据.json'}
   write(folder/'测试数据.json',data);write(folder/'instance.json',meta)
 for r in (2,3):
  for case in CASES[r]:
   for n in range(1,9):
    api=ROUNDS[r]/'接口测试'/case/f'第{n}个测试实例'/'instance.json';src=json.loads(api.read_text(encoding='utf-8'))
    folder=ROUNDS[r]/'功能测试'/case/f'第{n}个测试实例';folder.mkdir(parents=True,exist_ok=True)
    inputs=[{'步骤':i+1,'输入':json.loads((api.parent/st['bodyFile']).read_text(encoding='utf-8'))} for i,st in enumerate(src['steps']) if 'bodyFile' in st]
    # 页面执行核验业务结果；接口专属畸形分页另在接口目录验证。
    title=src['title'];expect='页面正常操作与状态展示符合总用例规则；具体数据见测试数据.json。'
    if case in ('R304','R307','R308') and ('分页' in title):title='首页与末页分页展示';expect='首页、末页与空结果显示正确，翻页不会重复或丢失记录。'
    if case=='R308' and n==7:title='不存在的搜索结果';expect='输入nomatch后客户列表为空，可重置恢复。'
    if case=='R305' and n==7:title='描述编辑与安全展示';expect='描述中输入的普通文字正确显示，不执行脚本或外部图片。'
    meta={'id':f'{case}-F{n:02d}','caseId':case,'iteration':r,'type':'功能测试','mode':'manual','title':title,'preconditions':'启动前后端，测试专用目录保存业务数据；默认卖家admin/123456；每例使用干净测试数据。','designMethod':src.get('designMethod','场景法'),'steps':['按测试数据.json准备本实例账号、商品、队列及图片。',UI[case],f'执行场景：{title}。','记录界面提示、业务状态、列表内容及截图；实际结果人工填写。'],'expected':FUNCTION_EXPECTATIONS[case][n-1],'dataFile':'测试数据.json','result':'待执行'}
    write(folder/'测试数据.json',{'对应接口实例':src['id'],'场景':title,'输入集合':inputs,'搜索或页面尺寸':'q=nomatch；分页首页和末页' if '分页' in title or '搜索结果' in title else '按场景输入'})
    write(folder/'instance.json',meta)
 for r,root in ROUNDS.items():
  for p in root.rglob('instance.json'):
   obj=json.loads(p.read_text(encoding='utf-8'));obj.setdefault('designMethod','场景法');obj.setdefault('type',p.parent.parent.parent.name);obj.setdefault('iteration',r);write(p,obj)
   if obj.get('mode')=='manual':
    text=f'# {obj["id"]} {obj["title"]}\n\n前置条件：{obj.get("preconditions","")}\n\n'
    text+='\n'.join(f'{i}. {step}' for i,step in enumerate(obj['steps'],1))+f'\n\n预期结果：{obj["expected"]}\n\n测试方法：{obj["designMethod"]}\n\n实际结果：待执行\n'
    (p.parent/'操作步骤.md').write_text(text,encoding='utf-8')
  (root/'README.md').write_text(f'# 第{r}轮测试数据\n\n按 `功能测试或接口测试/总用例编号/第n个测试实例/` 保存；每个总用例8个实例。接口实例由JUnit读取请求文件执行；功能实例为网页人工测试，执行前均标记待执行。\n',encoding='utf-8')
def strengthen():
 for case,n in [('R306',1),('R306',6),('R307',1),('R308',8)]:
  folder=ROUNDS[3]/'接口测试'/case/f'第{n}个测试实例';obj=json.loads((folder/'instance.json').read_text(encoding='utf-8'))
  temp=Scenario(3,case,n,obj['title']);temp.meta=obj
  if case=='R306' and n==1:
   for key,value in [('phone','bad'),('phone','1'*31),('defaultLocation',' '),('defaultLocation','地'*201)]:
    body={'username':'invalid'+key,'password':'Buyer1234','phone':'13800000001','defaultLocation':'学校南门'};body[key]=value
    temp.step('POST','/api/customer/register',400,body,session=None)
   temp.register('limitplace',location='地'*200,key='limit')
  elif case=='R306':
   for length,status in [(8,201),(128,201),(129,400)]:
    temp.step('POST','/api/customer/register',status,{'username':'pwd'+str(length),'password':'a'*length,'phone':'13800000001','defaultLocation':'北门'},session=None)
  elif case=='R307':
   for location in ['', '地'*201]:temp.step('POST','/api/customer/products/${p}/intents',400,{'name':'客户联系人','phone':'13900000001','meetingLocation':location},session='buyer')
  else:
   temp.step('POST','/api/seller/login',body={'username':'admin','password':'123456'},session='buyer',save='combined')
   temp.step('GET','/api/customer/session',session='combined',expect={'/authenticated':True})
   temp.step('POST','/api/seller/logout',204,session='combined')
   temp.step('GET','/api/customer/session',session='combined',expect={'/authenticated':True})
   temp.step('GET','/api/seller/customers',401,session='combined')
  temp.save()

def functional_assets():
 for round,base in ROUNDS.items():
  for meta_file in (base/'功能测试').rglob('instance.json'):
   obj=json.loads(meta_file.read_text(encoding='utf-8'));case=obj['caseId'];n=int(meta_file.parent.name[1:meta_file.parent.name.index('个')]);folder=meta_file.parent;names=[]
   if round==1 and case=='U06':
    if n in (2,6,7):names=['sample.png']
    elif n in (3,8):names=['sample.jpg','sample.png']
    elif n==4:names=['invalid.txt']
    elif n==5:names=['oversized.png']
   elif round==1 and (case=='U01' and n==3 or case=='U05' and n==4):names=['sample.png']
   elif round==3 and case=='R305':
    names=['sample.png','sample.jpg'] if n in (1,3,7,8) else [f'sample{i:02d}.png' for i in range(1,10 if n in (2,5) else 11)] if n in (2,5,6) else []
    if n==2:names+=['invalid.txt','oversized.png']
   for name in names:
    target=folder/name
    if name=='invalid.txt':target.write_text('用于错误格式测试，不是图片。',encoding='utf-8')
    elif name=='oversized.png':
     if not target.exists():
      Image.new('RGB',(16,16),(60,100,140)).save(target,'PNG')
      with target.open('ab') as out:out.truncate(5*1024*1024+1)
    else:Image.new('RGB',(32,32),(60+(n*10)%180,110,160)).save(target,'JPEG' if name.endswith('.jpg') else 'PNG')
   data_path=folder/obj['dataFile'];inputs=json.loads(data_path.read_text(encoding='utf-8'));inputs['本实例素材文件']=names
   if names:inputs['素材使用说明']='本文件夹中的图片供网页选择上传；oversized.png为5MB加1字节；invalid.txt用于错误格式。'
   write(data_path,inputs)

def main():
 migrate();first_additional();second();third_a1();third_a2();strengthen();functional();functional_assets()
 for r,p in ROUNDS.items():print(f'第{r}轮：接口{len(list((p/"接口测试").rglob("instance.json")))}，功能{len(list((p/"功能测试").rglob("instance.json")))}')
if __name__=='__main__':main()
