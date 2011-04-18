<%@ taglib uri="http://rhn.redhat.com/rhn" prefix="rhn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<html:xhtml/>
<html>
<head>
    <meta name="page-decorator" content="none" />
</head>
<%-- disableAutoComplete() hack added to prevent certain browsers from exposing sensitive data --%>
<body onLoad="disableAutoComplete();">
<rhn:toolbar base="h1" img="/img/rhn-icon-preferences.gif"
 helpUrl="/rhn/help/reference/en-US/s1-sm-your-rhn.jsp#s2-sm-your-rhn-prefs"
 imgAlt="preferences.jsp.alt">
<bean:message key="Your Preferences"/>
</rhn:toolbar>
<html:form action="/account/PrefSubmit">
<%@ include file="/WEB-INF/pages/common/fragments/user/preferences.jspf" %>
</html:form>
</body>
</html>
