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

**Upload Activity**: Navigate to the **Library** page, click **Add Activity**, and select a PDF. The system extracts structured metadata (title, description, age range, duration, format, materials, topics, Bloom level, cognitive/physical load) and concurrently generates teaching documents: Artikulationsschema, Deckblatt, Hintergrundwissen, Übung, and Lösungsblatt.

**Review & Correct**: Check the extracted metadata and adjust any errors before confirming. Once confirmed, the system finalises document generation in the background. The activity can be published from the draft view once you are satisfied.

**Publish**: Open the draft, review the generated documents, make any edits, and click **Publish** to make the activity visible to teachers.

<div style={{ textAlign: 'center', marginTop: '1.5rem' }}>
  <img src={useBaseUrl('/img/web/upload.png')} alt="Activity Upload page" style={{ width: '95%', borderRadius: '12px', border: '1px solid #000'}} />
  <p style={{ fontSize: '0.9rem', color: '#666', marginTop: '0.5rem' }}>
    Activity Upload page
  </p>
</div>
