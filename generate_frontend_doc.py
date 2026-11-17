from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH

doc = Document()

# 主标题
title = doc.add_heading('前端开发工作清单（单卖家排队购物系统）', level=0)
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
subtitle = doc.add_paragraph('个人负责内容与实现方式说明')
subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER

doc.add_paragraph()

# 1. 工程基础搭建
doc.add_heading('一、前端工程基础搭建', level=1)
table1 = doc.add_table(rows=1, cols=2)
table1.style = 'Table Grid'
table1.rows[0].cells[0].text = '工作内容'
table1.rows[0].cells[1].text = '实现方式'
items1 = [
    ('项目初始化', '基于Vite搭建Vue3前端工程，安装基础依赖，配置项目目录结构'),
    ('路由配置', '使用Vue Router配置页面路由，拆分买家首页、卖家登录页、卖家后台页三个核心页面'),
    ('接口封装', '用Axios封装所有后端请求接口，配置自动携带登录Cookie，统一处理请求报错和错误提示'),
    ('跨域代理配置', '在Vite配置文件中设置开发代理，将前端/api、/uploads请求自动转发到本地8080后端，解决跨域问题'),
]
for a, b in items1:
    row = table1.add_row().cells
    row[0].text = a
    row[1].text = b

# 2. 买家端页面开发
doc.add_heading('二、买家端页面开发（公开首页）', level=1)
table2 = doc.add_table(rows=1, cols=2)
table2.style = 'Table Grid'
table2.rows[0].cells[0].text = '功能点'
table2.rows[0].cells[1].text = '实现方式'
items2 = [
    ('商品信息展示', '对接后端当前商品接口，用Element Plus卡片组件渲染商品名称、价格、描述和图片；无在售商品时显示空状态占位'),
    ('排队意向提交', '实现姓名、联系电话输入表单，前端做必填校验；提交成功后弹窗显示系统生成的唯一排队口令码，提供一键复制功能'),
    ('口令码状态查询', '实现口令码输入框，查询后展示当前排队位置、意向状态和商品状态'),
    ('撤销排队', '排队状态下支持用户主动撤销排队，操作前弹出二次确认弹窗；已进入交易的用户自动拦截撤销操作并提示'),
]
for a, b in items2:
    row = table2.add_row().cells
    row[0].text = a
    row[1].text = b

# 3. 卖家后台页面开发
doc.add_heading('三、卖家后台页面开发', level=1)
table3 = doc.add_table(rows=1, cols=2)
table3.style = 'Table Grid'
table3.rows[0].cells[0].text = '功能点'
table3.rows[0].cells[1].text = '实现方式'
items3 = [
    ('卖家登录', '实现账号密码登录表单，对接后端登录接口，登录成功后保存会话跳转后台，错误密码给出对应提示'),
    ('后台导航', '实现左侧侧边菜单，包含概览、商品管理、排队买家三个功能入口，右上角放置退出登录按钮'),
    ('商品管理', '1. 新商品发布：名称、价格必填校验，支持描述填写和商品图片上传\n2. 商品编辑：拉取已有商品信息填充表单，修改后提交更新\n3. 图片上传：前端预校验图片格式（JPG/PNG）和大小（≤5MB），上传成功后关联到商品\n4. 商品下架：交易中状态自动禁用下架按钮'),
    ('排队队列管理', '1. 拉取排队列表按提交顺序展示所有等待买家的姓名、电话、提交时间、状态\n2. 支持作废指定买家、将买家重新排到队尾，所有操作加二次确认弹窗\n3. 提供「叫下一位」按钮，点击后自动选择队首买家开启交易'),
    ('交易结果登记', '支持登记交易成功/失败结果；交易失败时让卖家选择作废该买家或将其重新排到队尾，提交后刷新队列状态'),
]
for a, b in items3:
    row = table3.add_row().cells
    row[0].text = a
    row[1].text = b

# 4. 通用组件与交互优化
doc.add_heading('四、通用组件与体验优化', level=1)
table4 = doc.add_table(rows=1, cols=2)
table4.style = 'Table Grid'
table4.rows[0].cells[0].text = '工作内容'
table4.rows[0].cells[1].text = '实现方式'
items4 = [
    ('通用组件封装', '封装顶部导航、页面标题、内容卡片、状态标签、空状态占位等公共组件，所有页面复用，保持视觉风格统一'),
    ('统一弹窗', '封装通用确认弹窗组件，所有危险操作（撤销、作废、下架）都弹出二次确认，避免用户误操作'),
    ('响应式适配', '调整页面布局和间距，保证在窄屏/手机浏览器上页面元素不挤压、按钮可正常点击'),
    ('交互状态提示', '所有操作增加加载动画、成功/失败轻提示，避免用户重复点击提交'),
]
for a, b in items4:
    row = table4.add_row().cells
    row[0].text = a
    row[1].text = b

# 5. 收尾验证
doc.add_heading('五、开发收尾验证', level=1)
doc.add_paragraph('完成前后端全量联调，执行npm run build生产打包验证无编译错误，走通从买家提交排队、卖家叫号到交易完成的完整业务流程。')

doc.save(r'D:\Desktop\software\Software-Engineering-Practice\tests\前端工作清单.docx')
print('生成完成')
