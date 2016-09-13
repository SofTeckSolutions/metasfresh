package de.metas.ui.web.menu;

import java.util.Enumeration;
import java.util.Properties;

import org.adempiere.ad.security.IUserRolePermissions;
import org.adempiere.ad.security.IUserRolePermissionsDAO;
import org.adempiere.ad.security.UserRolePermissionsKey;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Services;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.model.X_AD_Menu;
import org.compiere.util.DB;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;

import de.metas.logging.LogManager;
import de.metas.ui.web.menu.MenuNode.MenuNodeType;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2016 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public final class MenuTreeLoader
{
	/* package */static MenuTreeLoader newInstance()
	{
		return new MenuTreeLoader();
	}

	// services
	private static final transient Logger logger = LogManager.getLogger(MenuTreeLoader.class);
	private final transient IUserRolePermissionsDAO userRolePermissionsDAO = Services.get(IUserRolePermissionsDAO.class);

	private static final int DEPTH_Root = 0;
	private static final int DEPTH_RootChildren = 1;

	private Properties _ctx;
	private UserRolePermissionsKey _userRolePermissionsKey;
	private IUserRolePermissions _userRolePermissions; // lazy

	private MenuTreeLoader()
	{
		super();
	}

	public MenuTreeLoader setCtx(final Properties ctx)
	{
		_ctx = ctx;
		return this;
	}

	private Properties getCtx()
	{
		Preconditions.checkNotNull(_ctx, "ctx");
		return _ctx;
	}

	public MenuTreeLoader setUserRolePermissionsKey(final UserRolePermissionsKey userRolePermissionsKey)
	{
		_userRolePermissionsKey = userRolePermissionsKey;
		return this;
	}

	private IUserRolePermissions getUserRolePermissions()
	{
		if (_userRolePermissions == null)
		{
			final UserRolePermissionsKey userRolePermissionsKey = _userRolePermissionsKey != null ? _userRolePermissionsKey : UserRolePermissionsKey.of(getCtx());
			_userRolePermissions = userRolePermissionsDAO.retrieveUserRolePermissions(userRolePermissionsKey);
		}
		return _userRolePermissions;
	}

	public MenuTree load()
	{
		if (logger.isTraceEnabled())
		{
			logger.trace("Loading menu tree for {}", getUserRolePermissions());
		}

		final MTreeNode rootNodeModel = retrieveRootNodeModel();
		final MenuNode rootNode = createMenuNodeRecursivelly(rootNodeModel, DEPTH_Root);
		if (rootNode == null)
		{
			throw new IllegalStateException("No root menu node available"); // shall not happen
		}

		return MenuTree.of(rootNode);
	}

	private MenuNode createMenuNodeRecursivelly(final MTreeNode nodeModel, final int depth)
	{
		final MenuNode.Builder nodeBuilder = createMenuNodeBuilder(nodeModel, depth);
		if (nodeBuilder == null)
		{
			logger.trace("Skip creating menu node for {}", nodeModel);
			return null;
		}

		final Enumeration<?> childModels = nodeModel.children();
		while (childModels.hasMoreElements())
		{
			final MTreeNode childModel = (MTreeNode)childModels.nextElement();
			
			final MenuNode childNode = createMenuNodeRecursivelly(childModel, depth + 1);
			if (childNode == null)
			{
				continue;
			}
			
			if(childModel.isCreateNewRecord())
			{
				final MenuNode childNodeNewRecord = createNewRecordNode(childNode);
				if(childNodeNewRecord != null)
				{
					nodeBuilder.addChildToFirstsList(childNodeNewRecord);
				}
			}

			nodeBuilder.addChild(childNode);
		}

		return nodeBuilder.build();
	}

	private MenuNode.Builder createMenuNodeBuilder(final MTreeNode nodeModel, final int depth)
	{
		final MenuNode.Builder builder = MenuNode.builder()
				.setId(nodeModel.getNode_ID())
				.setCaption(nodeModel.getName());

		final String action = nodeModel.getImageIndiactor();
		if (nodeModel.isSummary())
		{
			builder.setType(MenuNodeType.Group, -1);
		}
		else if (depth == DEPTH_RootChildren)
		{
			logger.warn("Skip creating leaf nodes for root: {}", nodeModel);
			return null;
		}
		else if (X_AD_Menu.ACTION_Window.equals(action))
		{
			builder.setType(MenuNodeType.Window, nodeModel.getAD_Window_ID());
		}
		else if (X_AD_Menu.ACTION_Process.equals(action))
		{
			builder.setType(MenuNodeType.Process, nodeModel.getAD_Process_ID());
		}
		else if (X_AD_Menu.ACTION_Report.equals(action))
		{
			builder.setType(MenuNodeType.Report, nodeModel.getAD_Process_ID());
		}
		else
		{
			return null;
		}

		return builder;
	}

	private MenuNode createNewRecordNode(final MenuNode node)
	{
		if (node.getType() != MenuNodeType.Window)
		{
			return null;
		}

		return MenuNode.builder()
				.setId(node.getId() + "-new")
				.setCaption("New " + node.getCaption()) // TODO: trl
				.setType(MenuNodeType.NewRecord, node.getElementId())
				.build();
	}

	private MTreeNode retrieveRootNodeModel()
	{
		final int adTreeId = retrieveAD_Tree_ID();
		if (adTreeId < 0)
		{
			throw new AdempiereException("Menu tree not found");
		}

		final MTree mTree = MTree.builder()
				.setCtx(getCtx())
				.setTrxName(ITrx.TRXNAME_None)
				.setAD_Tree_ID(adTreeId)
				.setEditable(false)
				.setClientTree(true)
				.build();
		final MTreeNode rootNodeModel = mTree.getRoot();
		return rootNodeModel;
	}

	private int retrieveAD_Tree_ID()
	{
		// metas: 03019: begin
		final IUserRolePermissions userRolePermissions = getUserRolePermissions();
		if (!userRolePermissions.hasPermission(IUserRolePermissions.PERMISSION_MenuAvailable))
		{
			return -1;
		}
		// metas: 03019: end

		int AD_Tree_ID = DB.getSQLValue(ITrx.TRXNAME_None,
				"SELECT COALESCE(r.AD_Tree_Menu_ID, ci.AD_Tree_Menu_ID)"
						+ "FROM AD_ClientInfo ci"
						+ " INNER JOIN AD_Role r ON (ci.AD_Client_ID=r.AD_Client_ID) "
						+ "WHERE AD_Role_ID=?",
				userRolePermissions.getAD_Role_ID());
		if (AD_Tree_ID <= 0)
		{
			AD_Tree_ID = 10; // Menu // FIXME: hardcoded
		}
		return AD_Tree_ID;
	}
}
