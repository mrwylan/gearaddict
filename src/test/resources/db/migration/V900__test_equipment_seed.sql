-- V900__test_equipment_seed.sql
-- Test-only seed data for the equipment catalog. Referenced by view tests that
-- exercise catalog search / selection (e.g. UC-009). Picked IDs via the
-- equipment_seq sequence so they do not collide with equipment rows created
-- by application code at test time.

INSERT INTO equipment (id, name, manufacturer, category, description) VALUES
    (nextval('equipment_seq'), 'Minimoog Model D', 'Moog',      'Synth',     'Classic monophonic analog synthesizer.'),
    (nextval('equipment_seq'), 'Prophet-5',       'Sequential', 'Synth',     'Five-voice polyphonic analog synthesizer.'),
    (nextval('equipment_seq'), 'RE-201 Space Echo','Roland',    'Effect',    'Tape delay and reverb unit.'),
    (nextval('equipment_seq'), 'Scarlett 2i2',    'Focusrite',  'Interface', 'Two-channel USB audio interface.');
