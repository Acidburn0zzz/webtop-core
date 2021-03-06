/*
 * WebTop Services is a Web Application framework developed by Sonicle S.r.l.
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICLE, SONICLE DISCLAIMS THE
 * WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle@sonicle.com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * Sonicle logo and Sonicle copyright notice. If the display of the logo is not
 * reasonably feasible for technical reasons, the Appropriate Legal Notices must
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
package com.sonicle.webtop.core.app;

import com.sonicle.webtop.core.sdk.AuthException;
import com.sonicle.webtop.core.sdk.WTException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.secnod.shiro.jaxrs.ShiroExceptionMapper;
import org.secnod.shiro.jersey.SubjectFactory;

/**
 *
 * @author malbinola
 */
public class RestApiJaxRsApplication extends ResourceConfig {
	
	public RestApiJaxRsApplication() {
		super();
		
		WebTopApp wta = WebTopApp.getInstance();
		if (wta != null) {
			register(new SubjectFactory());
			//register(new ServiceModelProcessor());
			register(new ShiroExceptionMapper());
			register(new AuthExceptionMapper());
			register(new WTExceptionMapper());
			
			// Register primary application API endpoint
			register(new RestApi(wta));
			// Loads API endpoints implementation dinamically
			ServiceManager svcm = wta.getServiceManager();
			for (String serviceId : svcm.listRegisteredServices()) {
				ServiceDescriptor desc = svcm.getDescriptor(serviceId);
				if (!desc.hasRestApiEndpoints()) continue;
				
				for (ServiceDescriptor.ApiEndpointClass apiClass : desc.getRestApiEndpoints()) {
					registerResources(Resource.builder(apiClass.clazz).path(apiClass.path).build());
				}
			}
		}
	}
	
	/*
	private static class ServiceModelProcessor implements ModelProcessor {
		
		@Override
		public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
			ResourceModel.Builder newModelBuilder = new ResourceModel.Builder(false);
			
			for (final Resource resource: resourceModel.getResources()) {
				final Class handlerClass = resource.getHandlerClasses().iterator().next();
				System.out.println(resource.getPath());
				final String newPath = WT.findServiceId(handlerClass) + "/" + StringUtils.removeStart(resource.getPath(), "/");
				System.out.println(newPath);
				final Resource.Builder resourceBuilder = Resource.builder(resource);
				resourceBuilder.path(newPath);
				newModelBuilder.addResource(resourceBuilder.build());
			}
			
			return newModelBuilder.build();
		}

		@Override
		public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
			return subResourceModel;
		}
	}
	*/
	
	private static class AuthExceptionMapper implements ExceptionMapper<AuthException> {

		@Override
		public Response toResponse(AuthException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
		}
	}
	
	private static class WTExceptionMapper implements ExceptionMapper<WTException> {

		@Override
		public Response toResponse(WTException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}
}
