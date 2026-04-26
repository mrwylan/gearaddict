-- V900__test_equipment_seed.sql
-- Test-only seed data for the equipment catalog. Referenced by view tests that
-- exercise catalog search / selection (e.g. UC-009). Picked IDs via the
-- equipment_seq sequence so they do not collide with equipment rows created
-- by application code at test time.

INSERT INTO manufacturer (name) VALUES
    ('Moog'),
    ('Sequential'),
    ('Roland'),
    ('Focusrite');

INSERT INTO equipment (id, name, manufacturer_id, category, description) VALUES
    (nextval('equipment_seq'), 'Minimoog Model D',  (SELECT id FROM manufacturer WHERE name = 'Moog'),       'Synth',     'Classic monophonic analog synthesizer.'),
    (nextval('equipment_seq'), 'Prophet-5',         (SELECT id FROM manufacturer WHERE name = 'Sequential'), 'Synth',     'Five-voice polyphonic analog synthesizer.'),
    (nextval('equipment_seq'), 'RE-201 Space Echo', (SELECT id FROM manufacturer WHERE name = 'Roland'),     'Effect',    'Tape delay and reverb unit.'),
    (nextval('equipment_seq'), 'Scarlett 2i2',      (SELECT id FROM manufacturer WHERE name = 'Focusrite'),  'Interface', 'Two-channel USB audio interface.');
