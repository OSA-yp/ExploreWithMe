SELECT format('CREATE DATABASE %I', 'ewm_stats_db')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ewm_stats_db') \gexec

SELECT format('CREATE DATABASE %I', 'ewm_event')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ewm_event') \gexec

SELECT format('CREATE DATABASE %I', 'ewm_request')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ewm_request') \gexec

SELECT format('CREATE DATABASE %I', 'ewm_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ewm_user') \gexec

SELECT format('CREATE DATABASE %I', 'ewm_compilation')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ewm_compilation') \gexec

SELECT format('CREATE DATABASE %I', 'ewm_category')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ewm_category') \gexec

SELECT format('CREATE DATABASE %I', 'ewm_comment')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ewm_comment') \gexec
