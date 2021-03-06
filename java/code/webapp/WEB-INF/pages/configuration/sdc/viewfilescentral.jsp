<%@ taglib uri="http://rhn.redhat.com/rhn" prefix="rhn" %>
<%@ taglib uri="http://rhn.redhat.com/tags/list" prefix="rl" %>
<%@ taglib uri="http://rhn.redhat.com/tags/config-managment" prefix="cfg" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<head>
    <meta name="name" value="sdc.config.jsp.header" />
</head>
<body>
<%@ include file="/WEB-INF/pages/common/fragments/systems/system-header.jspf" %>
<rhn:toolbar base="h2" icon="header-configuration">
  <bean:message key="sdc.config.header.overview"/>
</rhn:toolbar>
<p>
        <bean:message key="sdc.config.file_list.central_description"
                                        arg0="${fn:escapeXml(requestScope.system.name)}"/>
</p>

<rl:listset name="fileSet">
    <rhn:csrf />
    <rhn:submitted />

        <!-- Start of Files list -->
        <rl:list decorator="SelectableDecorator"
             width="100%"
             filter="com.redhat.rhn.frontend.action.configuration.sdc.ViewModifyPathsFilter"
             emptykey = "channelfiles.jsp.noFiles"
                 >
                    <rl:selectablecolumn value="${current.selectionKey}"
                                                selected="${current.selected}"/>

                <!-- File name column -->
                <rl:column sortable="true"
                           headerkey="sdc.config.file_list.name"
                           sortattr="path">
            <c:choose>
               <c:when test="${current.localRevision != null}">
                                <cfg:file path = "${current.path}"
                                                                                type="${current.localConfigFileType}" nolink="true"/>
               </c:when>
               <c:otherwise>
               <cfg:file path = "${current.path}"
                                type="${current.configFileType}" nolink="true"/>
               </c:otherwise>
            </c:choose>
                </rl:column>

                <c:set var="fileUrl" value="${cfg:fileUrl(current.configFileId)}"/>
                <c:set var="compareUrl" value="${cfg:fileCompareUrl(current.configFileId)}"/>


                <!-- Actions -->
                <rl:column bound="false"
                           headerkey="sdc.config.file_list.actions"
                                        >
                        <bean:message key="sdc.config.file_list.view_or_compare"
                                                                arg0 ="${fileUrl}" arg1="${compareUrl}"/>
                </rl:column>


                <!-- Provided By -->
        <rl:column bound="false"
               headerkey="sdc.config.file_list.provided_by"
               sortable="true"
               sortattr="channelNameDisplay"
                                >
                        <cfg:channel id = "${current.configChannelId}"
                                                        name ="${current.channelNameDisplay}"
                                                        type = "central"/>
        </rl:column>


        <!-- Overriden By -->
                <!-- Current Revision -->
                <rl:column bound="false"
                           headerkey="sdc.config.file_list.current_revision"
                                        >

                        <c:set var = "revisionLook">
                                        <bean:message key="sdcconfigfiles.jsp.filerev"
                                                                arg0="${current.configRevision}"/>
                        </c:set>
                        <c:set var="display"><cfg:file path ="${revisionLook}"
                                                type ="${current.configFileType}"
                                                id = "${current.configFileId}"
                                                revisionId = "${current.configRevisionId}"
                                                />
                        </c:set>

                        <c:choose>
                                <c:when test="${current.localRevision != null}">
                                        <span class="overridden-file">${display}</span>
                                </c:when>
                                        <c:otherwise>${display}</c:otherwise>
                        </c:choose>
                </rl:column>
        </rl:list>
</rl:listset>
</body>
</html>
