/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import javax.annotation.concurrent.Immutable;

import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.usergroup.IUserGroupManager;

@Immutable
public final class AppSecurity
{
  private AppSecurity ()
  {}

  public static void init ()
  {
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final IUserGroupManager aUserGroupMgr = PhotonSecurityManager.getUserGroupMgr ();

    // Standard users
    if (!aUserMgr.containsWithID (CSecurity.USER_ADMINISTRATOR_ID))
      aUserMgr.createPredefinedUser (CSecurity.USER_ADMINISTRATOR_ID,
                                     CSecurity.USER_ADMINISTRATOR_EMAIL,
                                     CSecurity.USER_ADMINISTRATOR_EMAIL,
                                     CSecurity.USER_ADMINISTRATOR_PASSWORD,
                                     CSecurity.USER_ADMINISTRATOR_NAME,
                                     null,
                                     null,
                                     null,
                                     null,
                                     false);

    // User group Administrators
    if (!aUserGroupMgr.containsWithID (CApp.USERGROUPID_SUPERUSER))
    {
      aUserGroupMgr.createPredefinedUserGroup (CApp.USERGROUPID_SUPERUSER, "Super users", null, null);
      // Assign administrator user to UG administrators
      aUserGroupMgr.assignUserToUserGroup (CSecurity.USERGROUP_ADMINISTRATORS_ID, CSecurity.USER_ADMINISTRATOR_ID);
    }
  }
}
