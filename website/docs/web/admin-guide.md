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

New activities are created and prepared on the **Drafts** page (in the admin sidebar). The lifecycle is **Upload → Review → Publish**.

**Upload Activity**: On the **Drafts** page, start a new draft and select a PDF. The system extracts structured metadata (title, description, age range, duration, format, materials, topics, Bloom level, cognitive/physical load) and concurrently generates teaching documents: Cover Sheet, Lesson Plan, Background Knowledge, Exercise, and Solution sheet.

**Review & Correct**: Open the draft to check the extracted metadata and generated documents. Edit anything that needs fixing before publishing.

**Publish**: When you are satisfied, publish the draft to make the activity visible to teachers in the Library and available to the recommendation engine.

<div style={{ textAlign: 'center', marginTop: '1.5rem' }}>
  <img src={useBaseUrl('/img/web/drafts.png')} alt="Drafts overview page" style={{ width: '95%', borderRadius: '12px', border: '1px solid #000'}} />
  <p style={{ fontSize: '0.9rem', color: '#666', marginTop: '0.5rem' }}>
    The Drafts overview, where activities are uploaded, reviewed, and published
  </p>
</div>
