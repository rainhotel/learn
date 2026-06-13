<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    int continuousSum(int n) {
        int sum = 0;
        for (int i = 1; i <= n; i++) {
            sum += i;
        }
        return sum;
    }
%>
<%
    String str = request.getParameter("number");
    if (str == null || str.trim().isEmpty()) {
        str = "10";
    }

    int r = 10;
    String message = null;
    try {
        r = Integer.parseInt(str.trim());
        if (r < 0) {
            message = "请输入一个非负整数。";
        }
    } catch (NumberFormatException e) {
        message = "输入格式错误，请输入整数。";
        r = 10;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>计算连续和</title>
</head>
<body>
    <h1>请输入一个自然数</h1>
    <form name="form1" method="post" action="">
        <input type="text" name="number" value="<%= str %>">
        <input type="submit" name="Submit1" value="计算">
    </form>

    <%
        if (message != null) {
    %>
        <p style="color: red;"><%= message %></p>
    <%
        } else {
    %>
        <p><%= r %> 的连续和是 <%= continuousSum(r) %></p>
    <%
        }
    %>
</body>
</html>
