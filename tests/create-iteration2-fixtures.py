"""第二轮三项改进及 A.1 数据生成器。实例文件供 MockMvc 直接读取。"""
from pathlib import Path
import json
from PIL import Image
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'第二轮验收测试数据'
CASES={'R201':'等待买家修改联系信息','R202':'按商品手动冻结与解冻','R203':'卖家密码修改与会话撤销','R204':'多商品发布与独立队列','R205':'两级分类管理','R206':'库存扣减与售罄下架','R207':'浏览搜索与分类分页','R208':'多图与富媒体描述'}
def write(path,obj):path.write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf-8')
class Scenario:
 def __init__(self,case,n,title):
  self.folder=OUT/case/f'第{n}个测试实例';self.folder.mkdir(parents=True,exist_ok=True)
  self.meta={'id':f'{case}-{n:02d}','caseId':case,'title':title,'mode':'automated','steps':[],'expected':'全部步骤满足状态码和响应字段断言'}
 def step(self,method,path,status=200,payload=None,expect=None,capture=None,session='seller',saveSession=None,file=None,mime=None):
  row={'method':method,'path':path,'status':status}
  if session:row['session']=session
  if saveSession:row['saveSession']=saveSession
  if payload is not None:
   name=f'请求数据{len(self.meta["steps"])+1:02d}.json';write(self.folder/name,payload);row['bodyFile']=name
  if file:row.update(file=file,mime=mime)
  if expect:row['assertions']=expect
  if capture:row['capture']=capture
  self.meta['steps'].append(row)
 def login(self,password='123456',name='seller'):
  self.step('POST','/api/seller/login',payload={'username':'admin','password':password},session=None,saveSession=name,expect={'/authenticated':True})
 def product(self,name='商品',stock=2,key='p',category=None,images=None,description='商品说明'):
  body={'name':name,'price':88.5,'stock':stock,'categoryId':category,'images':images or [],'description':description}
  self.step('POST','/api/seller/products',201,body,capture={key:'/id'})
 def submit(self,p='p',key='code',name='买家'):
  self.step('POST',f'/api/public/products/${{{p}}}/intents',201,{'name':name,'phone':'13800000001'},session=None,capture={key:'/passcode'},expect={'/position':1})
 def category(self,name,parent=None,key='cat'):
  self.step('POST','/api/seller/categories',201,{'name':name,'parentId':parent},capture={key:'/id'})
 def start(self,p='p',key='trade'):
  self.step('POST',f'/api/seller/products/${{{p}}}/trades/start',capture={key:'/id'})
 def save(self):write(self.folder/'instance.json',self.meta)
 def image(self,name,fmt):Image.new('RGB',(12,12),(70,110,160)).save(self.folder/name,fmt)
 def upload(self,name,key,mime='image/png',status=200):self.step('UPLOAD','/api/seller/products/image',status,file=name,mime=mime,capture={key:'/url'} if status==200 else None)
for n in range(1,4):
 s=Scenario('R201',n,['正常修改并保留排位','手动冻结期间修改','他人交易期间修改'][n-1]);s.login();s.product(stock=3);s.submit(key='first',name='队首')
 s.step('POST','/api/public/products/${p}/intents',201,{'name':'等待买家','phone':'13800000002'},session=None,capture={'code':'/passcode'},expect={'/position':2})
 if n==2:s.step('POST','/api/seller/products/${p}/freeze')
 if n==3:
  s.start();s.step('PUT','/api/public/intents/${first}',409,{'name':'不应修改','phone':'13900000001'},session=None)
 s.step('PUT','/api/public/intents/${code}',payload={'name':f'更新姓名{n}','phone':f'1390000000{n}'},session=None,expect={'/name':f'更新姓名{n}','/position':1 if n==3 else 2})
 s.step('PUT','/api/public/intents/${code}',400,{'name':' ','phone':'bad'},session=None)
 s.step('DELETE','/api/public/intents/${code}',204,session=None)
 s.step('GET','/api/public/intents/${code}',404,session=None);s.save()
for n in range(1,4):
 s=Scenario('R202',n,['冻结保留队列且其他商品可接单','解冻恢复交易且队列顺序不变','交易冻结不可手动解除'][n-1]);s.login();s.product(stock=n);s.product('其他商品',key='other');s.submit()
 if n==3:
  s.start();s.step('POST','/api/seller/products/${p}/unfreeze',409);s.step('POST','/api/seller/products/${p}/freeze',409)
 else:
  s.step('POST','/api/seller/products/${p}/freeze',expect={'/status':'MANUALLY_FROZEN'})
  s.step('GET','/api/public/intents/${code}',session=None,expect={'/position':1})
  s.step('POST','/api/public/products/${p}/intents',409,{'name':'新买家','phone':'13800000002'},session=None)
  s.submit('other','otherCode');s.step('POST','/api/seller/products/${p}/delist',409)
  s.step('POST','/api/seller/products/${p}/unfreeze',expect={'/status':'ON_SALE'});s.start()
 s.save()
for n in range(1,4):
 s=Scenario('R203',n,['原密码错误被拒绝','新密码长度限制','改密撤销全部旧会话'][n-1]);s.login();s.login(name='other')
 if n==1:s.step('PUT','/api/seller/password',400,{'currentPassword':'wrong','newPassword':'new-password-2026'});s.step('GET','/api/seller/session',expect={'/authenticated':True})
 elif n==2:s.step('PUT','/api/seller/password',400,{'currentPassword':'123456','newPassword':'short'});s.step('GET','/api/seller/session',expect={'/authenticated':True})
 else:
  s.step('PUT','/api/seller/password',204,{'currentPassword':'123456','newPassword':'new-password-2026'})
  s.step('GET','/api/seller/products',401,session='other')
  s.step('POST','/api/seller/login',401,{'username':'admin','password':'123456'},session=None)
  s.login('new-password-2026','new');s.step('GET','/api/seller/products',session='new',expect={'/total':0})
 s.save()
for n in range(1,4):
 s=Scenario('R204',n,f'同时发布{n+1}件商品并分别排队');s.login()
 for i in range(n+1):s.product(f'商品{i+1}',key=f'p{i}');s.submit(f'p{i}',f'code{i}',f'买家{i+1}')
 s.step('GET','/api/public/products',session=None,expect={'/total':n+1})
 s.step('GET','/api/public/products/current',409,session=None)
 s.step('POST','/api/seller/products/${p0}/trades/start',capture={'trade':'/id'})
 s.step('GET','/api/public/intents/${code0}',session=None,expect={'/status':'IN_TRADE'})
 s.step('GET','/api/public/intents/${code1}',session=None,expect={'/status':'WAITING','/position':1})
 s.step('POST','/api/seller/trades/${trade}/result',204,{'success':True})
 s.step('GET','/api/public/products/${p1}',session=None,expect={'/stock':2,'/status':'ON_SALE'});s.save()
for n in range(1,4):
 s=Scenario('R205',n,['分类关联与父类筛选','禁止三级循环和同级重名','分类改名及未引用分类删除'][n-1]);s.login();s.category('数码',key='root');s.category('键盘','${root}','child')
 if n==1:
  s.product(category='${child}');s.step('GET','/api/public/products?categoryId=${root}',session=None,expect={'/total':1,'/items/0/categoryName':'键盘'})
  s.step('DELETE','/api/seller/categories/${child}',409);s.step('DELETE','/api/seller/categories/${root}',409)
 elif n==2:
  s.step('POST','/api/seller/categories',400,{'name':'三级','parentId':'${child}'})
  s.step('PUT','/api/seller/categories/${root}',400,{'name':'数码','parentId':'${root}'})
  s.step('POST','/api/seller/categories',409,{'name':'键盘','parentId':'${root}'})
  s.category('家居',key='other');s.step('PUT','/api/seller/categories/${root}',409,{'name':'数码','parentId':'${other}'})
 else:
  s.step('PUT','/api/seller/categories/${child}',payload={'name':'机械键盘','parentId':'${root}'},expect={'/name':'机械键盘'})
  s.step('DELETE','/api/seller/categories/${child}',204);s.step('DELETE','/api/seller/categories/${root}',204);s.step('GET','/api/public/categories',session=None,expect={'':[]})
 s.save()
for n in range(1,4):
 s=Scenario('R206',n,['两件库存逐次成交售罄','失败重排不扣库存且重复结果不再扣','零库存补货上架与非法库存'][n-1]);s.login();s.product(stock=[2,1,0][n-1])
 if n==3:
  s.step('GET','/api/public/products',session=None,expect={'/total':0});s.step('POST','/api/seller/products/${p}/relist',409)
  for stock in [-1,0.5]:s.step('PUT','/api/seller/products/${p}',400,{'name':'商品','price':88.5,'stock':stock})
  s.step('PUT','/api/seller/products/${p}',payload={'name':'商品','price':88.5,'stock':3},expect={'/status':'DELISTED','/stock':3})
  s.step('POST','/api/seller/products/${p}/relist',expect={'/status':'ON_SALE','/stock':3});s.submit();s.start()
  s.step('PUT','/api/seller/products/${p}',409,{'name':'商品','price':88.5,'stock':8})
 else:
  s.submit(key='code');s.step('POST','/api/public/products/${p}/intents',201,{'name':'第二位','phone':'13800000002'},session=None,capture={'second':'/passcode'},expect={'/position':2});s.start()
  if n==2:
   s.step('POST','/api/seller/trades/${trade}/result',204,{'success':False,'disposition':'REQUEUE'})
   s.step('GET','/api/public/products/${p}',session=None,expect={'/stock':1});s.start(key='trade2')
   s.step('POST','/api/seller/trades/${trade2}/result',204,{'success':True});s.step('POST','/api/seller/trades/${trade2}/result',409,{'success':True})
  else:
   s.step('POST','/api/seller/trades/${trade}/result',204,{'success':True});s.step('GET','/api/public/products/${p}',session=None,expect={'/stock':1,'/status':'ON_SALE'})
   s.step('GET','/api/public/intents/${second}',session=None,expect={'/position':1});s.start(key='trade2');s.step('POST','/api/seller/trades/${trade2}/result',204,{'success':True})
  s.step('GET','/api/public/products/${p}',404,session=None);s.step('GET','/api/seller/products/${p}/workbench',expect={'/product/stock':0,'/product/status':'DELISTED'})
 s.save()
for n in range(1,4):
 s=Scenario('R207',n,['商品名称搜索和下架排除','描述关键词忽略大小写','分类包含子类并分页'][n-1]);s.login();s.category('数码',key='root');s.category('键盘','${root}','child')
 s.product('Keyboard红轴',category='${child}',description='<p>USB Keyboard Good</p>');s.product('鼠标',key='other',category='${root}',description='办公');s.product('已下架键盘',key='gone');s.step('POST','/api/seller/products/${gone}/delist',204)
 if n==1:s.step('GET','/api/public/products?q=Keyboard',session=None,expect={'/total':1,'/items/0/name':'Keyboard红轴'})
 elif n==2:s.step('GET','/api/public/products?q=usb',session=None,expect={'/total':1});s.step('GET','/api/public/products?q=notfound',session=None,expect={'/total':0})
 else:
  s.step('GET','/api/public/products?categoryId=${root}&page=0&size=1',session=None,expect={'/total':2,'/items/0/name':'鼠标'})
  s.step('GET','/api/public/products?categoryId=${root}&page=1&size=1',session=None,expect={'/total':2,'/items/0/name':'Keyboard红轴'})
  s.step('GET','/api/public/products?page=-1',400,session=None);s.step('GET','/api/public/products?size=101',400,session=None)
 s.step('GET','/api/customer/session',404,session=None);s.step('GET','/api/seller/customers',404);s.save()
for n in range(1,4):
 s=Scenario('R208',n,['两张图片与格式化描述','九图边界与错误文件拒绝','描述图片二次保存及危险内容清理'][n-1]);s.login();s.image('商品图.png','PNG');s.image('商品图.jpg','JPEG');s.upload('商品图.png','image');s.upload('商品图.jpg','image2','image/jpeg')
 if n==1:
  s.product(images=['${image}','${image2}'],description='<h3>介绍</h3><p><strong>九成新</strong></p>');s.step('GET','/api/public/products/${p}',session=None,expect={'/images':['${image}','${image2}'],'/description':'<h3>介绍</h3><p><strong>九成新</strong></p>'})
 elif n==2:
  images=['${image}']*9;s.product(images=images);s.step('GET','/api/public/products/${p}',session=None,expect={'/images':images})
  s.step('PUT','/api/seller/products/${p}',400,{'name':'商品','price':88.5,'images':images+['${image2}']})
  (s.folder/'错误格式.txt').write_text('not an image',encoding='utf-8');s.upload('错误格式.txt','bad','text/plain',400)
  (s.folder/'超大图片.png').write_bytes(b'\x00'*(5*1024*1024+1));s.upload('超大图片.png','large','image/png',400)
 else:
  s.product(images=['${image}'],description='<p onclick="alert(1)">安全文字</p><img src="${image}"><script>alert(1)</script>')
  cleaned='<p>安全文字</p><img src="${image}" alt="商品描述图片">alert(1)'
  s.step('GET','/api/public/products/${p}',session=None,expect={'/description':cleaned},capture={'description':'/description'})
  s.step('PUT','/api/seller/products/${p}',payload={'name':'商品','price':88.5,'images':['${image}'],'description':'${description}'},expect={'/description':cleaned})
  s.step('PUT','/api/seller/products/${p}',400,{'name':'商品','price':88.5,'images':['javascript:alert(1)']})
 s.save()
print('第二轮 8 个用例、24 个实例及对应请求/图片文件已生成')
