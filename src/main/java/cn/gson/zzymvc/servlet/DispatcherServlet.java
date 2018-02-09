package cn.gson.zzymvc.servlet;

import cn.gson.zzymvc.annoation.Controller;
import cn.gson.zzymvc.annoation.RequestMapping;
import cn.gson.zzymvc.bean.ControllerBean;
import cn.gson.zzymvc.bean.ModelAndView;
import com.google.common.collect.Maps;
import org.reflections.Reflections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2018 soho team All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : cn.gson.zzymvc.servlet</li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2018年02月09日</li>
 * <li>@author     : zzy0_0</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
public class DispatcherServlet extends HttpServlet {

    private Map<String, ControllerBean> urlMap = Maps.newHashMap();

    @Override
    public void init() throws ServletException {
        //扫描所有的类并且判断哪些类使用了controller注解
        Reflections reflections = new Reflections("cn.gson.controller");
        Set<Class<?>> classSet = reflections.getTypesAnnotatedWith(Controller.class);
        for (Class<?> aClass : classSet) {
            Method[] methods = aClass.getDeclaredMethods();
            if (methods != null && methods.length > 0) {
                for (Method method : methods) {
                    RequestMapping rm = method.getAnnotation(RequestMapping.class);
                    if (rm != null) {
                        String url = rm.value();
                        urlMap.put(url, new ControllerBean(aClass, method));
                    }
                }
            }
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestURI = req.getRequestURI();
        if ("/favicon.ico".equals(requestURI)) {
            return;
        }
        if (requestURI != null && requestURI.contains(".")) {
            requestURI = requestURI.substring(0, requestURI.lastIndexOf("."));
            System.out.println(requestURI);
        }
        ControllerBean controllerBean = urlMap.get(requestURI);
        if (controllerBean == null) {
            req.getRequestDispatcher(requestURI).forward(req, resp);
            return;
        }
        System.out.println(urlMap);
        Class controllerClazz = controllerBean.getControllerClass();
        Method method = controllerBean.getControllerMethod();
        try {
            Object instance = controllerClazz.newInstance();
            Object result = method.invoke(instance);
            if (result != null && result.getClass() == ModelAndView.class) {
                this.handlerResult(req, resp, (ModelAndView) result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handlerResult(HttpServletRequest req, HttpServletResponse resp, ModelAndView modelAndView) throws ServletException, IOException {
        String viewName = modelAndView.getViewName();
        Map<String, Object> model = modelAndView.getModel();
        Set<Map.Entry<String, Object>> entries = model.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String modelName = entry.getKey();
            Object value = entry.getValue();
            req.setAttribute(modelName, value);
            System.out.println(modelName);
        }
        String prefix = "/WEB-INF/jsp/";
        String suffix = ".jsp";
        req.getRequestDispatcher(prefix + viewName + suffix).forward(req, resp);
    }
}