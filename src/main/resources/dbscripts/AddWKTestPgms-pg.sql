-- set search_path to kutu;
-- Test Programm-Extensions
-- KuTu TG Allgäu Kür & Pflicht
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into disziplin (id, name) values (2, 'Pferd Pauschen') on conflict (id) do nothing;
insert into disziplin (id, name) values (3, 'Ring') on conflict (id) do nothing;
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into disziplin (id, name) values (6, 'Reck') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values 
(44, 'KuTu TG Allgäu Kür & Pflicht', 0, null, 44, 0, 100, 'c6592fdf-1b60-4334-8db0-a678a637e699', 1)
,(45, 'Kür', 1, 44, 45, 0, 100, '0131ab0a-d275-445c-860a-5b4fce152653', 1)
,(46, 'WK I Kür', 1, 45, 46, 0, 100, '9e6045ce-b9f7-4c91-b54a-5ec50d18628b', 1)
,(47, 'WK II LK1', 1, 45, 47, 0, 100, 'cba44d99-0544-4a4b-b9c5-a97a00e2b7a5', 1)
,(48, 'WK III LK1', 1, 45, 48, 16, 17, 'a0209937-dc49-4d56-b8cf-5998cbf96a53', 1)
,(49, 'WK IV LK2', 1, 45, 49, 14, 15, 'f414e11d-9943-44c6-9267-99b4568bebb2', 1)
,(50, 'Pflicht', 1, 44, 50, 0, 100, 'c6f67d97-a913-4a82-a1c4-e1684191e41a', 1)
,(51, 'WK V Jug', 1, 50, 51, 14, 18, '91026459-df00-45ee-a44f-ad2e81a815ec', 1)
,(52, 'WK VI Schüler A', 1, 50, 52, 12, 13, '64f80cf6-2a30-4a77-80d0-5c7bd3ee0737', 1)
,(53, 'WK VII Schüler B', 1, 50, 53, 10, 11, '3799370c-eff9-42cd-9868-16d36ecc1a53', 1)
,(54, 'WK VIII Schüler C', 1, 50, 54, 8, 9, 'bde177ad-90be-489c-9120-fb1974722f00', 1)
,(55, 'WK IX Schüler D', 1, 50, 55, 0, 7, 'f4bc8841-6f2d-47af-b52c-72fc8ab63c0e', 1) on conflict(id) do update set name=excluded.name, aggregate=excluded.aggregate, parent_id=excluded.parent_id, ord=excluded.ord, alter_von=excluded.alter_von, alter_bis=excluded.alter_bis, riegenmode=excluded.riegenmode;
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values 
(154, 46, 1, '', '', 1.0, 1, 0, 0, 3, 1, 0, 30, 1)
,(155, 46, 2, '', '', 1.0, 1, 0, 1, 3, 1, 0, 30, 1)
,(156, 46, 3, '', '', 1.0, 1, 0, 2, 3, 1, 0, 30, 1)
,(157, 46, 4, '', '', 1.0, 1, 0, 3, 3, 1, 0, 30, 1)
,(158, 46, 5, '', '', 1.0, 1, 0, 4, 3, 1, 0, 30, 1)
,(159, 46, 6, '', '', 1.0, 1, 0, 5, 3, 1, 0, 30, 1)
,(160, 47, 1, '', '', 1.0, 1, 0, 6, 3, 1, 0, 30, 1)
,(161, 47, 2, '', '', 1.0, 1, 0, 7, 3, 1, 0, 30, 1)
,(162, 47, 3, '', '', 1.0, 1, 0, 8, 3, 1, 0, 30, 1)
,(163, 47, 4, '', '', 1.0, 1, 0, 9, 3, 1, 0, 30, 1)
,(164, 47, 5, '', '', 1.0, 1, 0, 10, 3, 1, 0, 30, 1)
,(165, 47, 6, '', '', 1.0, 1, 0, 11, 3, 1, 0, 30, 1)
,(166, 48, 1, '', '', 1.0, 1, 0, 12, 3, 1, 0, 30, 1)
,(167, 48, 2, '', '', 1.0, 1, 0, 13, 3, 1, 0, 30, 1)
,(168, 48, 3, '', '', 1.0, 1, 0, 14, 3, 1, 0, 30, 1)
,(169, 48, 4, '', '', 1.0, 1, 0, 15, 3, 1, 0, 30, 1)
,(170, 48, 5, '', '', 1.0, 1, 0, 16, 3, 1, 0, 30, 1)
,(171, 48, 6, '', '', 1.0, 1, 0, 17, 3, 1, 0, 30, 1)
,(172, 49, 1, '', '', 1.0, 1, 0, 18, 3, 1, 0, 30, 1)
,(173, 49, 2, '', '', 1.0, 1, 0, 19, 3, 1, 0, 30, 1)
,(174, 49, 3, '', '', 1.0, 1, 0, 20, 3, 1, 0, 30, 1)
,(175, 49, 4, '', '', 1.0, 1, 0, 21, 3, 1, 0, 30, 1)
,(176, 49, 5, '', '', 1.0, 1, 0, 22, 3, 1, 0, 30, 1)
,(177, 49, 6, '', '', 1.0, 1, 0, 23, 3, 1, 0, 30, 1)
,(178, 51, 1, '', '', 1.0, 1, 0, 24, 3, 1, 0, 30, 1)
,(179, 51, 2, '', '', 1.0, 1, 0, 25, 3, 1, 0, 30, 1)
,(180, 51, 3, '', '', 1.0, 1, 0, 26, 3, 1, 0, 30, 1)
,(181, 51, 4, '', '', 1.0, 1, 0, 27, 3, 1, 0, 30, 1)
,(182, 51, 5, '', '', 1.0, 1, 0, 28, 3, 1, 0, 30, 1)
,(183, 51, 6, '', '', 1.0, 1, 0, 29, 3, 1, 0, 30, 1)
,(184, 52, 1, '', '', 1.0, 1, 0, 30, 3, 1, 0, 30, 1)
,(185, 52, 2, '', '', 1.0, 1, 0, 31, 3, 1, 0, 30, 1)
,(186, 52, 3, '', '', 1.0, 1, 0, 32, 3, 1, 0, 30, 1)
,(187, 52, 4, '', '', 1.0, 1, 0, 33, 3, 1, 0, 30, 1)
,(188, 52, 5, '', '', 1.0, 1, 0, 34, 3, 1, 0, 30, 1)
,(189, 52, 6, '', '', 1.0, 1, 0, 35, 3, 1, 0, 30, 1)
,(190, 53, 1, '', '', 1.0, 1, 0, 36, 3, 1, 0, 30, 1)
,(191, 53, 2, '', '', 1.0, 1, 0, 37, 3, 1, 0, 30, 1)
,(192, 53, 3, '', '', 1.0, 1, 0, 38, 3, 1, 0, 30, 1)
,(193, 53, 4, '', '', 1.0, 1, 0, 39, 3, 1, 0, 30, 1)
,(194, 53, 5, '', '', 1.0, 1, 0, 40, 3, 1, 0, 30, 1)
,(195, 53, 6, '', '', 1.0, 1, 0, 41, 3, 1, 0, 30, 1)
,(196, 54, 1, '', '', 1.0, 1, 0, 42, 3, 1, 0, 30, 1)
,(197, 54, 2, '', '', 1.0, 1, 0, 43, 3, 1, 0, 30, 1)
,(198, 54, 3, '', '', 1.0, 1, 0, 44, 3, 1, 0, 30, 1)
,(199, 54, 4, '', '', 1.0, 1, 0, 45, 3, 1, 0, 30, 1)
,(200, 54, 5, '', '', 1.0, 1, 0, 46, 3, 1, 0, 30, 1)
,(201, 54, 6, '', '', 1.0, 1, 0, 47, 3, 1, 0, 30, 1)
,(202, 55, 1, '', '', 1.0, 1, 0, 48, 3, 1, 0, 30, 1)
,(203, 55, 2, '', '', 1.0, 1, 0, 49, 3, 1, 0, 30, 1)
,(204, 55, 3, '', '', 1.0, 1, 0, 50, 3, 1, 0, 30, 1)
,(205, 55, 4, '', '', 1.0, 1, 0, 51, 3, 1, 0, 30, 1)
,(206, 55, 5, '', '', 1.0, 1, 0, 52, 3, 1, 0, 30, 1)
,(207, 55, 6, '', '', 1.0, 1, 0, 53, 3, 1, 0, 30, 1) on conflict(id) do update set masculin=excluded.masculin, feminim=excluded.feminim, ord=excluded.ord, dnote=excluded.dnote, min=excluded.min, max=excluded.max, startgeraet=excluded.startgeraet;
-- KuTuRi TG Allgäu Kür & Pflicht
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into disziplin (id, name) values (27, 'Stufenbarren') on conflict (id) do nothing;
insert into disziplin (id, name) values (28, 'Balken') on conflict (id) do nothing;
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values 
(56, 'KuTuRi TG Allgäu Kür & Pflicht', 0, null, 56, 0, 100, 'b81b5ed8-8656-4abb-b64d-d9850531b35b', 1)
,(57, 'Kür', 1, 56, 57, 0, 100, '8403a2bf-1bf0-46b0-915b-43adf9dac15e', 1)
,(58, 'WK I Kür', 1, 57, 58, 0, 100, 'fc51cda7-3725-46c6-9828-461ecf2ab819', 1)
,(59, 'WK II LK1', 1, 57, 59, 0, 100, 'f853dffb-fd60-4e66-8f1e-fcb1888a261a', 1)
,(60, 'WK III LK1', 1, 57, 60, 16, 17, '54409c90-c8f6-4032-a55d-64930d918e7b', 1)
,(61, 'WK IV LK2', 1, 57, 61, 14, 15, '7575f0ef-041f-40ee-980b-92586371b965', 1)
,(62, 'Pflicht', 1, 56, 62, 0, 100, '6f1ee180-ca6b-4f66-8986-feace96498bf', 1)
,(63, 'WK V Jug', 1, 62, 63, 14, 18, '40134f8a-7b04-400c-93e4-8a16a4f493c7', 1)
,(64, 'WK VI Schüler A', 1, 62, 64, 12, 13, 'dc063ba7-c7ce-4202-afea-2c4610dea8d7', 1)
,(65, 'WK VII Schüler B', 1, 62, 65, 10, 11, '377dd29e-bcd6-4f97-b710-266ca7c3e482', 1)
,(66, 'WK VIII Schüler C', 1, 62, 66, 8, 9, '624c8e42-b82e-46ef-b2e6-60aae417443f', 1)
,(67, 'WK IX Schüler D', 1, 62, 67, 0, 7, 'c79769b6-9b16-4d3b-b9ff-b5dce68d9b43', 1) on conflict(id) do update set name=excluded.name, aggregate=excluded.aggregate, parent_id=excluded.parent_id, ord=excluded.ord, alter_von=excluded.alter_von, alter_bis=excluded.alter_bis, riegenmode=excluded.riegenmode;
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values 
(208, 58, 4, '', '', 1.0, 0, 1, 0, 3, 1, 0, 30, 1)
,(209, 58, 27, '', '', 1.0, 0, 1, 1, 3, 1, 0, 30, 1)
,(210, 58, 28, '', '', 1.0, 0, 1, 2, 3, 1, 0, 30, 1)
,(211, 58, 1, '', '', 1.0, 0, 1, 3, 3, 1, 0, 30, 1)
,(212, 59, 4, '', '', 1.0, 0, 1, 4, 3, 1, 0, 30, 1)
,(213, 59, 27, '', '', 1.0, 0, 1, 5, 3, 1, 0, 30, 1)
,(214, 59, 28, '', '', 1.0, 0, 1, 6, 3, 1, 0, 30, 1)
,(215, 59, 1, '', '', 1.0, 0, 1, 7, 3, 1, 0, 30, 1)
,(216, 60, 4, '', '', 1.0, 0, 1, 8, 3, 1, 0, 30, 1)
,(217, 60, 27, '', '', 1.0, 0, 1, 9, 3, 1, 0, 30, 1)
,(218, 60, 28, '', '', 1.0, 0, 1, 10, 3, 1, 0, 30, 1)
,(219, 60, 1, '', '', 1.0, 0, 1, 11, 3, 1, 0, 30, 1)
,(220, 61, 4, '', '', 1.0, 0, 1, 12, 3, 1, 0, 30, 1)
,(221, 61, 27, '', '', 1.0, 0, 1, 13, 3, 1, 0, 30, 1)
,(222, 61, 28, '', '', 1.0, 0, 1, 14, 3, 1, 0, 30, 1)
,(223, 61, 1, '', '', 1.0, 0, 1, 15, 3, 1, 0, 30, 1)
,(224, 63, 4, '', '', 1.0, 0, 1, 16, 3, 1, 0, 30, 1)
,(225, 63, 27, '', '', 1.0, 0, 1, 17, 3, 1, 0, 30, 1)
,(226, 63, 28, '', '', 1.0, 0, 1, 18, 3, 1, 0, 30, 1)
,(227, 63, 1, '', '', 1.0, 0, 1, 19, 3, 1, 0, 30, 1)
,(228, 64, 4, '', '', 1.0, 0, 1, 20, 3, 1, 0, 30, 1)
,(229, 64, 27, '', '', 1.0, 0, 1, 21, 3, 1, 0, 30, 1)
,(230, 64, 28, '', '', 1.0, 0, 1, 22, 3, 1, 0, 30, 1)
,(231, 64, 1, '', '', 1.0, 0, 1, 23, 3, 1, 0, 30, 1)
,(232, 65, 4, '', '', 1.0, 0, 1, 24, 3, 1, 0, 30, 1)
,(233, 65, 27, '', '', 1.0, 0, 1, 25, 3, 1, 0, 30, 1)
,(234, 65, 28, '', '', 1.0, 0, 1, 26, 3, 1, 0, 30, 1)
,(235, 65, 1, '', '', 1.0, 0, 1, 27, 3, 1, 0, 30, 1)
,(236, 66, 4, '', '', 1.0, 0, 1, 28, 3, 1, 0, 30, 1)
,(237, 66, 27, '', '', 1.0, 0, 1, 29, 3, 1, 0, 30, 1)
,(238, 66, 28, '', '', 1.0, 0, 1, 30, 3, 1, 0, 30, 1)
,(239, 66, 1, '', '', 1.0, 0, 1, 31, 3, 1, 0, 30, 1)
,(240, 67, 4, '', '', 1.0, 0, 1, 32, 3, 1, 0, 30, 1)
,(241, 67, 27, '', '', 1.0, 0, 1, 33, 3, 1, 0, 30, 1)
,(242, 67, 28, '', '', 1.0, 0, 1, 34, 3, 1, 0, 30, 1)
,(243, 67, 1, '', '', 1.0, 0, 1, 35, 3, 1, 0, 30, 1) on conflict(id) do update set masculin=excluded.masculin, feminim=excluded.feminim, ord=excluded.ord, dnote=excluded.dnote, min=excluded.min, max=excluded.max, startgeraet=excluded.startgeraet;
-- Turn10®-Verein
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into disziplin (id, name) values (28, 'Balken') on conflict (id) do nothing;
insert into disziplin (id, name) values (30, 'Minitramp') on conflict (id) do nothing;
insert into disziplin (id, name) values (6, 'Reck') on conflict (id) do nothing;
insert into disziplin (id, name) values (27, 'Stufenbarren') on conflict (id) do nothing;
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into disziplin (id, name) values (2, 'Pferd Pauschen') on conflict (id) do nothing;
insert into disziplin (id, name) values (31, 'Ringe') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values 
(68, 'Turn10®-Verein', 0, null, 68, 0, 100, 'eac48376-aeac-4241-8db5-d910bbae82f0', 3)
,(69, 'BS', 0, 68, 69, 0, 100, '197e83c9-7ca7-4bac-96e8-ac8be828f1ab', 3)
,(70, 'OS', 0, 68, 70, 0, 100, 'fbf335c2-89b4-44de-91bf-79f616180fe6', 3) on conflict(id) do update set name=excluded.name, aggregate=excluded.aggregate, parent_id=excluded.parent_id, ord=excluded.ord, alter_von=excluded.alter_von, alter_bis=excluded.alter_bis, riegenmode=excluded.riegenmode;
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values 
(244, 69, 1, '', '', 1.0, 1, 1, 0, 3, 1, 0, 20, 1)
,(245, 69, 5, '', '', 1.0, 1, 0, 1, 3, 1, 0, 20, 1)
,(246, 69, 28, '', '', 1.0, 0, 1, 2, 3, 1, 0, 20, 1)
,(247, 69, 30, '', '', 1.0, 1, 1, 3, 3, 1, 0, 20, 1)
,(248, 69, 6, '', '', 1.0, 1, 1, 4, 3, 1, 0, 20, 1)
,(249, 69, 27, '', '', 1.0, 0, 1, 5, 3, 1, 0, 20, 1)
,(250, 69, 4, '', '', 1.0, 1, 1, 6, 3, 1, 0, 20, 1)
,(251, 69, 2, '', '', 1.0, 1, 0, 7, 3, 1, 0, 20, 1)
,(252, 69, 31, '', '', 1.0, 1, 0, 8, 3, 1, 0, 20, 1)
,(253, 70, 1, '', '', 1.0, 1, 1, 9, 3, 1, 0, 20, 1)
,(254, 70, 5, '', '', 1.0, 1, 0, 10, 3, 1, 0, 20, 1)
,(255, 70, 28, '', '', 1.0, 0, 1, 11, 3, 1, 0, 20, 1)
,(256, 70, 30, '', '', 1.0, 1, 1, 12, 3, 1, 0, 20, 1)
,(257, 70, 6, '', '', 1.0, 1, 1, 13, 3, 1, 0, 20, 1)
,(258, 70, 27, '', '', 1.0, 0, 1, 14, 3, 1, 0, 20, 1)
,(259, 70, 4, '', '', 1.0, 1, 1, 15, 3, 1, 0, 20, 1)
,(260, 70, 2, '', '', 1.0, 1, 0, 16, 3, 1, 0, 20, 1)
,(261, 70, 31, '', '', 1.0, 1, 0, 17, 3, 1, 0, 20, 1) on conflict(id) do update set masculin=excluded.masculin, feminim=excluded.feminim, ord=excluded.ord, dnote=excluded.dnote, min=excluded.min, max=excluded.max, startgeraet=excluded.startgeraet;
-- Turn10®-Schule
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into disziplin (id, name) values (28, 'Balken') on conflict (id) do nothing;
insert into disziplin (id, name) values (6, 'Reck') on conflict (id) do nothing;
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values 
(71, 'Turn10®-Schule', 0, null, 71, 0, 100, 'f027daf4-c4a3-42e5-8338-16c5beafa479', 2)
,(72, 'BS', 0, 71, 72, 0, 100, '43c48811-d02c-43fc-ac2c-fada22603543', 2)
,(73, 'OS', 0, 71, 73, 0, 100, '1c444a9a-ea7e-4e4d-9f5f-90d4b00966ff', 2) on conflict(id) do update set name=excluded.name, aggregate=excluded.aggregate, parent_id=excluded.parent_id, ord=excluded.ord, alter_von=excluded.alter_von, alter_bis=excluded.alter_bis, riegenmode=excluded.riegenmode;
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values 
(262, 72, 1, '', '', 1.0, 1, 1, 0, 3, 1, 0, 20, 1)
,(263, 72, 5, '', '', 1.0, 1, 0, 1, 3, 1, 0, 20, 1)
,(264, 72, 28, '', '', 1.0, 0, 1, 2, 3, 1, 0, 20, 1)
,(265, 72, 6, '', '', 1.0, 1, 1, 3, 3, 1, 0, 20, 1)
,(266, 72, 4, '', '', 1.0, 1, 1, 4, 3, 1, 0, 20, 1)
,(267, 73, 1, '', '', 1.0, 1, 1, 5, 3, 1, 0, 20, 1)
,(268, 73, 5, '', '', 1.0, 1, 0, 6, 3, 1, 0, 20, 1)
,(269, 73, 28, '', '', 1.0, 0, 1, 7, 3, 1, 0, 20, 1)
,(270, 73, 6, '', '', 1.0, 1, 1, 8, 3, 1, 0, 20, 1)
,(271, 73, 4, '', '', 1.0, 1, 1, 9, 3, 1, 0, 20, 1) on conflict(id) do update set masculin=excluded.masculin, feminim=excluded.feminim, ord=excluded.ord, dnote=excluded.dnote, min=excluded.min, max=excluded.max, startgeraet=excluded.startgeraet;
-- GeTu BLTV
insert into disziplin (id, name) values (6, 'Reck') on conflict (id) do nothing;
insert into disziplin (id, name) values (1, 'Boden') on conflict (id) do nothing;
insert into disziplin (id, name) values (26, 'Schaukelringe') on conflict (id) do nothing;
insert into disziplin (id, name) values (4, 'Sprung') on conflict (id) do nothing;
insert into disziplin (id, name) values (5, 'Barren') on conflict (id) do nothing;
insert into programm (id, name, aggregate, parent_id, ord, alter_von, alter_bis, uuid, riegenmode) values 
(74, 'GeTu BLTV', 0, null, 74, 0, 100, '829ea6f3-daa1-41f0-8422-8131258d4e20', 1)
,(75, 'K1', 0, 74, 75, 0, 10, '624e3abb-c0c4-415b-a7f1-43e6c12ea5fc', 1)
,(76, 'K2', 0, 74, 76, 0, 12, 'f5839635-7086-4d89-b254-f006bcb5be21', 1)
,(77, 'K3', 0, 74, 77, 0, 14, 'e3e31aa7-cf09-40ec-a7dd-b1d10ffe2aee', 1)
,(78, 'K4', 0, 74, 78, 0, 16, '99860754-8f03-4987-887f-19e4897b9e8a', 1)
,(79, 'K5', 0, 74, 79, 0, 100, '7e6c37ca-7b27-4531-845f-5655ec3fec52', 1)
,(80, 'K6', 0, 74, 80, 0, 100, '66481152-9a5d-456c-ab26-5563ba42934b', 1)
,(81, 'K7', 0, 74, 81, 0, 100, 'a8752867-3890-451f-a4e6-41ac176ece68', 1)
,(82, 'KD', 0, 74, 82, 22, 100, 'd0d0c3db-347f-4a11-8dad-8d17fa237806', 1)
,(83, 'KH', 0, 74, 83, 28, 100, '7b93d519-6003-4038-a1ac-78f19eb915e9', 1) on conflict(id) do update set name=excluded.name, aggregate=excluded.aggregate, parent_id=excluded.parent_id, ord=excluded.ord, alter_von=excluded.alter_von, alter_bis=excluded.alter_bis, riegenmode=excluded.riegenmode;
insert into wettkampfdisziplin (id, programm_id, disziplin_id, kurzbeschreibung, detailbeschreibung, notenfaktor, masculin, feminim, ord, scale, dnote, min, max, startgeraet) values 
(272, 75, 6, '', '', 1.0, 1, 1, 0, 3, 0, 0, 10, 1)
,(273, 75, 1, '', '', 1.0, 1, 1, 1, 3, 0, 0, 10, 1)
,(274, 75, 26, '', '', 1.0, 1, 1, 2, 3, 0, 0, 10, 1)
,(275, 75, 4, '', '', 1.0, 1, 1, 3, 3, 0, 0, 10, 1)
,(276, 75, 5, '', '', 1.0, 1, 0, 4, 3, 0, 0, 10, 0)
,(277, 76, 6, '', '', 1.0, 1, 1, 5, 3, 0, 0, 10, 1)
,(278, 76, 1, '', '', 1.0, 1, 1, 6, 3, 0, 0, 10, 1)
,(279, 76, 26, '', '', 1.0, 1, 1, 7, 3, 0, 0, 10, 1)
,(280, 76, 4, '', '', 1.0, 1, 1, 8, 3, 0, 0, 10, 1)
,(281, 76, 5, '', '', 1.0, 1, 0, 9, 3, 0, 0, 10, 0)
,(282, 77, 6, '', '', 1.0, 1, 1, 10, 3, 0, 0, 10, 1)
,(283, 77, 1, '', '', 1.0, 1, 1, 11, 3, 0, 0, 10, 1)
,(284, 77, 26, '', '', 1.0, 1, 1, 12, 3, 0, 0, 10, 1)
,(285, 77, 4, '', '', 1.0, 1, 1, 13, 3, 0, 0, 10, 1)
,(286, 77, 5, '', '', 1.0, 1, 0, 14, 3, 0, 0, 10, 0)
,(287, 78, 6, '', '', 1.0, 1, 1, 15, 3, 0, 0, 10, 1)
,(288, 78, 1, '', '', 1.0, 1, 1, 16, 3, 0, 0, 10, 1)
,(289, 78, 26, '', '', 1.0, 1, 1, 17, 3, 0, 0, 10, 1)
,(290, 78, 4, '', '', 1.0, 1, 1, 18, 3, 0, 0, 10, 1)
,(291, 78, 5, '', '', 1.0, 1, 0, 19, 3, 0, 0, 10, 0)
,(292, 79, 6, '', '', 1.0, 1, 1, 20, 3, 0, 0, 10, 1)
,(293, 79, 1, '', '', 1.0, 1, 1, 21, 3, 0, 0, 10, 1)
,(294, 79, 26, '', '', 1.0, 1, 1, 22, 3, 0, 0, 10, 1)
,(295, 79, 4, '', '', 1.0, 1, 1, 23, 3, 0, 0, 10, 1)
,(296, 79, 5, '', '', 1.0, 1, 0, 24, 3, 0, 0, 10, 0)
,(297, 80, 6, '', '', 1.0, 1, 1, 25, 3, 0, 0, 10, 1)
,(298, 80, 1, '', '', 1.0, 1, 1, 26, 3, 0, 0, 10, 1)
,(299, 80, 26, '', '', 1.0, 1, 1, 27, 3, 0, 0, 10, 1)
,(300, 80, 4, '', '', 1.0, 1, 1, 28, 3, 0, 0, 10, 1)
,(301, 80, 5, '', '', 1.0, 1, 0, 29, 3, 0, 0, 10, 0)
,(302, 81, 6, '', '', 1.0, 1, 1, 30, 3, 0, 0, 10, 1)
,(303, 81, 1, '', '', 1.0, 1, 1, 31, 3, 0, 0, 10, 1)
,(304, 81, 26, '', '', 1.0, 1, 1, 32, 3, 0, 0, 10, 1)
,(305, 81, 4, '', '', 1.0, 1, 1, 33, 3, 0, 0, 10, 1)
,(306, 81, 5, '', '', 1.0, 1, 0, 34, 3, 0, 0, 10, 0)
,(307, 82, 6, '', '', 1.0, 1, 1, 35, 3, 0, 0, 10, 1)
,(308, 82, 1, '', '', 1.0, 1, 1, 36, 3, 0, 0, 10, 1)
,(309, 82, 26, '', '', 1.0, 1, 1, 37, 3, 0, 0, 10, 1)
,(310, 82, 4, '', '', 1.0, 1, 1, 38, 3, 0, 0, 10, 1)
,(311, 82, 5, '', '', 1.0, 1, 0, 39, 3, 0, 0, 10, 0)
,(312, 83, 6, '', '', 1.0, 1, 1, 40, 3, 0, 0, 10, 1)
,(313, 83, 1, '', '', 1.0, 1, 1, 41, 3, 0, 0, 10, 1)
,(314, 83, 26, '', '', 1.0, 1, 1, 42, 3, 0, 0, 10, 1)
,(315, 83, 4, '', '', 1.0, 1, 1, 43, 3, 0, 0, 10, 1)
,(316, 83, 5, '', '', 1.0, 1, 0, 44, 3, 0, 0, 10, 0) on conflict(id) do update set masculin=excluded.masculin, feminim=excluded.feminim, ord=excluded.ord, dnote=excluded.dnote, min=excluded.min, max=excluded.max, startgeraet=excluded.startgeraet;

