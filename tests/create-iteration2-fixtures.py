"""兼容旧命令，统一按三轮范围生成测试资料。"""
from pathlib import Path
import runpy
if __name__ == '__main__':
    runpy.run_path(str(Path(__file__).with_name('generate-iteration-tests.py')), run_name='__main__')
