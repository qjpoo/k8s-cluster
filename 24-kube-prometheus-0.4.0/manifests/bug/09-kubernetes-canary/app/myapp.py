import prometheus_client
from prometheus_client import Counter, Gauge
from prometheus_client import Summary, CollectorRegistry
from flask import Response, Flask
import time
import random
import os


app = Flask(__name__)

# 定义一个注册器，注册器可以把指标都收集起来，然后最后返回注册器数据
REGISTRY = CollectorRegistry(auto_describe=False)

# 定义一个Counter类型的变量，这个变量不是指标名称，这种Counter类型只增加
# 不减少，程序重启的时候会被重新设置为0，构造函数第一个参数是定义 指标名称，
# 第二个是定义HELP中显示的内容，都属于文本
# 第三个参数是标签列表，也就是给这个指标加labels，这个也可以不设置
http_requests_total = Counter("http_requests", "Total request cout of the host", ['method', 'endpoint'], registry=REGISTRY)

# Summary类型，它可以统计2个时间
# request_processing_seconds_count 该函数被调用的数量
# request_processing_seconds_sum  该函数执行所花的时长
request_time = Summary('request_processing_seconds', 'Time spent processing request', registry=REGISTRY)


@app.route("/metrics")
def requests_count():
    """
    当访问/metrics这个URL的时候就执行这个方法，并返回相关信息。
    :return:
    """
    return Response(prometheus_client.generate_latest(REGISTRY),
                    mimetype="text/plain")

# 这个是健康检查用的
@app.route('/healthy')
def healthy():
    return "healthy"


@app.route('/')
@request_time.time()  # 这个必须要放在app.route的下面
def hello_world():
    # .inc()表示增加，默认是加1，你可以设置为加1.5，比如.inc(1.5)
    # http_requests_total.inc()
    # 下面这种写法就是为这个指标加上标签，但是这里的method和endpoint
    # 都在Counter初始化的时候放进去的。
    # 你想统计那个ULR的访问量就把这个放在哪里
    http_requests_total.labels(method="get", endpoint="/").inc()
    # 这里设置0-1之间随机数用于模拟页面响应时长
    time.sleep(random.random())
    html = "Hello World!" \
           "App Version: {version}"
    # 这里我会读取一个叫做VERSION的环境变量，
    # 这个变量会随Dockerfile设置到镜像中
    return html.format(version=os.getenv("VERSION", "888"))


if __name__ == '__main__':
    app.run(host="0.0.0.0", port="5555")
