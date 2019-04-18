# g4proxy


a http proxy witch use cell phone network


## 项目简介

这是一个代理服务共享系统，可以将多个android手机组建成一个http代理服务集群。

## 部署文档

### 服务器部署

1。 选择需要在公网上面的服务器，且需要有足够的端口资源。
2。 修改代码： com.virjar.g4proxyserver.G4proxyServerApplication
```
 ProxyInstanceHolder.g4ProxyServer = new G4ProxyServer(50000);
 ProxyInstanceHolder.g4ProxyServer.startUp();

```
如上代码，修改端口号为一个服务器地址

3. springboot 打包
执行命令
``./gradlew g4proxy-server:bootJar``

之后将会得到文件 ``g4proxy-server/build/libs/g4proxy-server-0.0.1-SNAPSHOT.jar ``

4. 将该文件上传至服务器，并启动他

``nohup java jar  path/to/g4proxy-server-0.0.1-SNAPSHOT.jar &``

### 客户端部署

当前客户端实现的是Android手机，配置方式如下

1. 配置服务器地址
修改代码： com.virjar.g4proxy.android.HttpProxyService.startServiceInternal

```
        G4ProxyClient g4ProxyClient = new G4ProxyClient("www.scumall.com", 50000, clientKey);
        g4ProxyClient.startup();
```
上述代码，修改为你的服务器ip，和刚刚设置的服务器端口

2. build apk
``./gradlew app:assembleRelease ``
将会得到apk文件 ``app/build/outputs/apk/release/app-release.apk``

3. 将apk安装到手机，并且配置成允许后台网络通信、允许开机自启动


### 使用

1. 获取手机端口列表。
手机连上服务器之后，每个手机将会映射为一个端口。得到端口和当前服务器host，便组装成为一个代理配置(host:port)
访问服务器的resetFul端口接口，得到端口列表，如： ``http://www.scumall.com:10000/portList``

2. 通过代理客户端访问代理服务

如下:
```
virjar-share:barchart-udt virjar$ curl -x www.scumall.com:24576 "https://www.baidu.com"
<!DOCTYPE html>
<!--STATUS OK--><html> <head><meta http-equiv=content-type content=text/html;charset=utf-8><meta http-equiv=X-UA-Compatible content=IE=Edge><meta content=always name=referrer><link rel=stylesheet type=text/css href=https://ss1.bdstatic.com/5eN1bjq8AAUYm2zgoY3K/r/www/cache/bdorz/baidu.min.css><title>百度一下，你就知道</title></head> <body link=#0000cc> <div id=wrapper> <div id=head> <div class=head_wrapper> <div class=s_form> <div class=s_form_wrapper> <div id=lg> <img hidefocus=true src=//www.baidu.com/img/bd_logo1.png width=270 height=129> </div> <form id=form name=f action=//www.baidu.com/s class=fm> <input type=hidden name=bdorz_come value=1> <input type=hidden name=ie value=utf-8> <input type=hidden name=f value=8> <input type=hidden name=rsv_bp value=1> <input type=hidden name=rsv_idx value=1> <input type=hidden name=tn value=baidu><span class="bg s_ipt_wr"><input id=kw name=wd class=s_ipt value maxlength=255 autocomplete=off autofocus=autofocus></span><span class="bg s_btn_wr"><input type=submit id=su value=百度一下 class="bg s_btn" autofocus></span> </form> </div> </div> <div id=u1> <a href=http://news.baidu.com name=tj_trnews class=mnav>新闻</a> <a href=https://www.hao123.com name=tj_trhao123 class=mnav>hao123</a> <a href=http://map.baidu.com name=tj_trmap class=mnav>地图</a> <a href=http://v.baidu.com name=tj_trvideo class=mnav>视频</a> <a href=http://tieba.baidu.com name=tj_trtieba class=mnav>贴吧</a> <noscript> <a href=http://www.baidu.com/bdorz/login.gif?login&amp;tpl=mn&amp;u=http%3A%2F%2Fwww.baidu.com%2f%3fbdorz_come%3d1 name=tj_login class=lb>登录</a> </noscript> <script>document.write('<a href="http://www.baidu.com/bdorz/login.gif?login&tpl=mn&u='+ encodeURIComponent(window.location.href+ (window.location.search === "" ? "?" : "&")+ "bdorz_come=1")+ '" name="tj_login" class="lb">登录</a>');
                </script> <a href=//www.baidu.com/more/ name=tj_briicon class=bri style="display: block;">更多产品</a> </div> </div> </div> <div id=ftCon> <div id=ftConw> <p id=lh> <a href=http://home.baidu.com>关于百度</a> <a href=http://ir.baidu.com>About Baidu</a> </p> <p id=cp>&copy;2017&nbsp;Baidu&nbsp;<a href=http://www.baidu.com/duty/>使用百度前必读</a>&nbsp; <a href=http://jianyi.baidu.com/ class=cp-feedback>意见反馈</a>&nbsp;京ICP证030173号&nbsp; <img src=//www.baidu.com/img/gs.gif> </p> </div> </div> </div> </body> </html>
virjar-share:barchart-udt virjar$
```