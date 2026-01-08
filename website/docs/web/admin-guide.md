---
sidebar_position: 7
title: Admin Guide
---
import useBaseUrl from '@docusaurus/useBaseUrl';

# Administrator Guide

Manage teachers, admins, and the activity library.

## User Management

**Create a User**: Click **Add User**, enter email, name, and role (Teacher or Admin), set a password, and click **Create User**. Teachers receive a credentials email; admins do not.

**Edit a User**: Select a user, click **Edit**, update details, and save.

**Delete a User**: Select a user, click **Delete**, and confirm. This removes all their saved favorites and search history.

<div style={{ textAlign: 'center', marginTop: '1.5rem' }}>
  <img src={useBaseUrl('/img/web/users.png')} alt="User Management page" style={{ width: '95%', borderRadius: '12px', border: '1px solid #000'}} />
  <p style={{ fontSize: '0.9rem', color: '#666', marginTop: '0.5rem' }}>
    User Management page
  </p>
</div>

## Activity Management

**Upload Activity**: Navigate to the **Library** page, click **Add Activity**, select a PDF, and the system automatically extracts metadata (title, description, age range, duration, format, materials, topics, Bloom level, cognitive/physical load).

**Review & Correct**: Check the extracted data and correct any errors before clicking **Confirm & Save**. The activity appears in the library immediately.

<div style={{ textAlign: 'center', marginTop: '1.5rem' }}>
  <img src={useBaseUrl('/img/web/upload.png')} alt="Activity Upload page" style={{ width: '95%', borderRadius: '12px', border: '1px solid #000'}} />
  <p style={{ fontSize: '0.9rem', color: '#666', marginTop: '0.5rem' }}>
    Activity Upload page
  </p>
</div>
