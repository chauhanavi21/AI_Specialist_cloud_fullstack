-- PostgreSQL seed data. Generated from src/main/resources/data.sql (the H2 dev seed).
-- Idempotent: every INSERT is ON CONFLICT DO NOTHING, so restarts never fail or duplicate rows.

-- Seed Users (Password is 'Password123!')
-- BCrypt for 'Password123!': $2a$10$eYaM.av7fpnqRNOsM.ogXuXxxarwO3Fc1EnY6rSulnA.Zqd8jPhh.
INSERT INTO users (id, email, password_hash, full_name, phone_number, gift_points_balance, wallet_balance, role, created_at)
VALUES
(1, 'alex.dev@ibm.com', '$2a$10$eYaM.av7fpnqRNOsM.ogXuXxxarwO3Fc1EnY6rSulnA.Zqd8jPhh.', 'Alex Rivera', '+91 98100 00144', 250, 1500.00, 'ROLE_CUSTOMER', CURRENT_TIMESTAMP),
(2, 'sarah.connor@example.com', '$2a$10$eYaM.av7fpnqRNOsM.ogXuXxxarwO3Fc1EnY6rSulnA.Zqd8jPhh.', 'Sarah Connor', '+91 98100 00188', 100, 0.00, 'ROLE_CUSTOMER', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
-- Seed Addresses
INSERT INTO addresses (id, user_id, recipient_name, email, phone_number, street_address, address_line2, city, state, postal_code, country, is_default)
VALUES
(1, 1, 'Alex Rivera', 'alex.dev@ibm.com', '+91 98100 00144', 'IBM India, Embassy Golf Links Business Park', 'Block B, Off Intermediate Ring Road', 'Bengaluru', 'Karnataka', '560071', 'India', TRUE),
(2, 1, 'Alex Rivera (Home)', 'alex.dev@ibm.com', '+91 98100 00144', '45 Tech Plaza, Apt 4B', 'Koramangala 5th Block', 'Bengaluru', 'Karnataka', '560095', 'India', FALSE)
ON CONFLICT DO NOTHING;
-- Seed Categories
INSERT INTO categories (id, name, slug, description, book_count)
VALUES
(1, 'Cloud & Containers', 'cloud-containers', 'Kubernetes, OpenShift, Red Hat, Docker, and hybrid cloud architecture.', 2),
(2, 'AI & Machine Learning', 'ai-ml', 'Generative AI, Agentic workflows, LLMs, and enterprise AI engineering.', 4),
(3, 'Microservices & APIs', 'microservices-apis', 'Spring Boot 3, RESTful design, event-driven architectures, and Kafka.', 3),
(4, 'DevOps & SRE', 'devops-sre', 'CI/CD pipelines, GitOps, observability, automated testing, and reliability.', 3),
(5, 'Software Engineering & Architecture', 'software-engineering', 'Domain-Driven Design, clean code, design patterns, and system design.', 3)
ON CONFLICT DO NOTHING;
-- Seed Publishers (Brands)
INSERT INTO publishers (id, name, code, website)
VALUES
(1, 'IBM Press', 'IBM_PRESS', 'https://www.ibm.com/press'),
(2, 'O''Reilly Media', 'OREILLY', 'https://www.oreilly.com'),
(3, 'Manning Publications', 'MANNING', 'https://www.manning.com'),
(4, 'Addison-Wesley', 'ADDISON_WESLEY', 'https://www.pearson.com'),
(5, 'Pragmatic Bookshelf', 'PRAGMATIC', 'https://pragprog.com')
ON CONFLICT DO NOTHING;
-- Seed Authors (photo_url is NULL: the UI draws an initials avatar, so no third-party images are loaded)
INSERT INTO authors (id, name, bio, photo_url)
VALUES
(1, 'Roland Huemer', 'Roland Huemer is a cloud architect who has led container platform migrations for large enterprises. He writes about resilient, mission-critical systems on Red Hat OpenShift and IBM Cloud, and teaches platform engineering teams how to run Kubernetes in regulated industries.', NULL),
(2, 'Elena Rostova', 'Elena Rostova builds multi-agent systems for enterprise engineering teams. Her work focuses on tool servers, reasoning loops and the guardrails that make autonomous assistants safe to run in production.', NULL),
(3, 'Thomas Vitale', 'Thomas Vitale is a software engineer and Java Champion who specialises in cloud native applications with Spring Boot and Kubernetes. He is a frequent conference speaker and open source contributor.', NULL),
(4, 'Bilgin Ibryam', 'Bilgin Ibryam is a product manager and architect who has spent years in the Kubernetes and integration space. He co-authored the book that catalogued the patterns behind cloud native applications.', NULL),
(5, 'Martin Kleppmann', 'Martin Kleppmann is a researcher and author who works on distributed systems and data-intensive applications. His writing is known for explaining hard consistency and storage trade-offs clearly.', NULL),
(6, 'Michael Curry', 'Michael Curry leads product for enterprise AI platforms. He writes about running large language models securely on hybrid cloud with proper data governance.', NULL),
(7, 'Chris Richardson', 'Chris Richardson is a software architect and consultant who created the microservices pattern language used by teams worldwide. He teaches sagas, API gateways and event-driven design.', NULL),
(8, 'Betsy Beyer', 'Betsy Beyer is a technical writer who documented site reliability engineering practices at scale, covering service level objectives, blameless postmortems and incident response.', NULL),
(9, 'Eric Evans', 'Eric Evans is the author of the book that introduced domain-driven design. He consults on tackling complexity in the heart of business software with a ubiquitous language and bounded contexts.', NULL),
(10, 'Luca Mezzalira', 'Luca Mezzalira is a chief architect who has scaled front end development across many autonomous teams. He writes about micro-frontends and modular web architecture.', NULL),
(11, 'Emily Freeman', 'Emily Freeman is a DevOps practitioner and speaker who focuses on declarative infrastructure, progressive delivery and keeping release pipelines boring.', NULL),
(12, 'Jay Alammar', 'Jay Alammar is known for visual explanations of machine learning. His writing walks through transformers, tokenization and embeddings step by step.', NULL),
(13, 'Robert Martin', 'Robert C. Martin, known as Uncle Bob, is a software craftsman who has written widely on clean code, dependency inversion and testable architecture.', NULL),
(14, 'Kevin Wittek', 'Kevin Wittek writes about integration testing for Java services using real databases and queues instead of fragile mocks.', NULL),
(15, 'David Chen', 'David Chen is an applied AI engineer who teaches developers how to write system prompts, structure model output and orchestrate agent loops.', NULL)
ON CONFLICT DO NOTHING;
-- Seed Books (prices in INR; cover_image_url is NULL so the UI draws a typographic cover)
INSERT INTO books (id, title, author, author_id, isbn, category_id, publisher_id, price, original_price, stock_quantity, rating, review_count, cover_image_url, description, published_year, launch_date, page_count, book_language, book_format, copies_sold, tentative_delivery_days, is_featured)
VALUES
(1, 'Enterprise Cloud Native Architecture with OpenShift', 'Roland Huemer & S. Ray', 1, '978-0134853444', 1, 1, 899.00, 1099.00, 45, 4.9, 142, NULL, 'Architecting resilient, containerized mission-critical systems across hybrid multi-cloud using Red Hat OpenShift, Kubernetes, and IBM Cloud.', 2024, CURRENT_DATE - INTERVAL '400 days', 480, 'English', 'Paperback', 920, 2, TRUE),
(2, 'Building Agentic AI Systems with LLMs', 'Elena Rostova & Avi Chauhan', 2, '978-1492098231', 2, 2, 999.00, 1199.00, 30, 4.9, 218, NULL, 'Practical handbook for building multi-agent autonomous engineering pipelines, MCP tool servers, and enterprise reasoning loops with watsonx and Bob.', 2025, CURRENT_DATE - INTERVAL '20 days', 410, 'English', 'Paperback', 1450, 2, TRUE),
(3, 'Cloud Native Spring in Action', 'Thomas Vitale', 3, '978-1617298813', 3, 3, 849.00, 999.00, 38, 4.8, 186, NULL, 'Hands-on guide to building production-grade, reactive, container-ready Spring Boot microservices with Kubernetes, PostgreSQL, and Testcontainers.', 2023, CURRENT_DATE - INTERVAL '700 days', 620, 'English', 'Paperback', 1980, 2, TRUE),
(4, 'Kubernetes Patterns & Cloud Native Best Practices', 'Bilgin Ibryam & Roland Huss', 4, '978-1492050285', 1, 2, 799.00, 949.00, 25, 4.7, 98, NULL, 'Reusable elements and design patterns for building cloud-native applications on container orchestrators.', 2023, CURRENT_DATE - INTERVAL '650 days', 312, 'English', 'eBook', 640, 3, FALSE),
(5, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 5, '978-1449373320', 5, 2, 749.00, 899.00, 60, 5.0, 580, NULL, 'The definitive guide to the storage, processing, and consensus engines behind modern high-throughput architectures.', 2022, CURRENT_DATE - INTERVAL '1100 days', 616, 'English', 'Hardcover', 4200, 2, TRUE),
(6, 'Generative AI on Hybrid Cloud', 'Michael J. Curry & Team', 6, '978-0138210982', 2, 1, 899.00, 1049.00, 22, 4.8, 89, NULL, 'Deploying enterprise LLMs securely on IBM Cloud, ROKS, and Red Hat Enterprise Linux with full data governance.', 2024, CURRENT_DATE - INTERVAL '60 days', 390, 'English', 'Paperback', 380, 2, TRUE),
(7, 'Microservices Patterns with Spring Boot & Java', 'Chris Richardson', 7, '978-1617294549', 3, 3, 849.00, 999.00, 33, 4.8, 165, NULL, 'Comprehensive guide to transactional sagas, API gateways, CQRS, and asynchronous event messaging.', 2023, CURRENT_DATE - INTERVAL '500 days', 520, 'English', 'Paperback', 1100, 3, FALSE),
(8, 'Site Reliability Engineering at Scale', 'Betsy Beyer & Niall Murphy', 8, '978-1491929124', 4, 2, 799.00, 949.00, 29, 4.9, 310, NULL, 'How enterprise organizations master service level objectives (SLOs), blameless postmortems, and incident response.', 2023, CURRENT_DATE - INTERVAL '450 days', 550, 'English', 'Hardcover', 2300, 2, FALSE),
(9, 'Domain-Driven Design Reference & Patterns', 'Eric Evans', 9, '978-0321125217', 5, 4, 949.00, 1149.00, 18, 4.9, 420, NULL, 'Tackling software complexity at the heart of business with ubiquitous language and bounded contexts.', 2022, CURRENT_DATE - INTERVAL '900 days', 560, 'English', 'Hardcover', 2750, 3, FALSE),
(10, 'Building Micro-Frontends with React & Vite', 'Luca Mezzalira', 10, '978-1492082996', 3, 2, 699.00, 849.00, 40, 4.6, 75, NULL, 'Scaling web development across autonomous squads with modern frontend modularity and state synchronization.', 2024, CURRENT_DATE - INTERVAL '90 days', 340, 'English', 'eBook', 310, 2, FALSE),
(11, 'Continuous Delivery Pipelines with GitOps', 'Emily Freeman', 11, '978-0136923485', 4, 4, 749.00, 899.00, 27, 4.7, 92, NULL, 'Declarative infrastructure management, automated rollbacks, and progressive delivery using ArgoCD.', 2024, CURRENT_DATE - INTERVAL '45 days', 380, 'English', 'Paperback', 270, 2, FALSE),
(12, 'Hands-On Large Language Models', 'Jay Alammar & Maarten Grootendorst', 12, '978-1098150969', 2, 2, 949.00, 1099.00, 35, 4.9, 195, NULL, 'Understanding transformer architectures, tokenization, embeddings, and fine-tuning with Python and PyTorch.', 2024, CURRENT_DATE - INTERVAL '120 days', 440, 'English', 'Paperback', 1620, 2, TRUE),
(13, 'Clean Architecture in Java 21', 'Robert C. Martin', 13, '978-0134494166', 5, 4, 729.00, 849.00, 50, 4.8, 380, NULL, 'A craftsman''s guide to software structure, dependency inversion, and testable enterprise architectures.', 2023, CURRENT_DATE - INTERVAL '300 days', 432, 'English', 'Paperback', 2480, 2, FALSE),
(14, 'Automated Testing with Testcontainers and Spring Boot', 'Kevin Wittek', 14, '978-1617299902', 4, 3, 699.00, 849.00, 20, 4.8, 64, NULL, 'Real database and queue integration testing without fragile mocks using Docker and Testcontainers in Java.', 2024, CURRENT_DATE - INTERVAL '10 days', 310, 'English', 'eBook', 120, 3, FALSE),
(15, 'Prompt Engineering & Agentic Workflows for Developers', 'David Chen & Liam Vance', 15, '978-1680509922', 2, 5, 599.00, 749.00, 42, 4.7, 110, NULL, 'Mastering system prompts, few-shot conditioning, structured schema generation, and agentic loop orchestration.', 2025, CURRENT_DATE - INTERVAL '5 days', 290, 'English', 'Paperback', 210, 2, TRUE)
ON CONFLICT DO NOTHING;
-- Seed Coupons
INSERT INTO coupons (id, code, description, discount_type, discount_value, min_order_amount, max_discount, active, expires_at)
VALUES
(1, 'WELCOME10', '10% off your basket (up to Rs 300)', 'PERCENT', 10.00, 0.00, 300.00, TRUE, NULL),
(2, 'BOOKS100', 'Flat Rs 100 off orders above Rs 999', 'FLAT', 100.00, 999.00, NULL, TRUE, NULL),
(3, 'CLOUD20', '20% off orders above Rs 1,500 (up to Rs 500)', 'PERCENT', 20.00, 1500.00, 500.00, TRUE, NULL)
ON CONFLICT DO NOTHING;
-- Seed reviews from the demo customers
INSERT INTO reviews (id, book_id, user_id, rating, comment_text, created_at)
VALUES
(1, 1, 2, 5, 'Clear, practical and full of real migration lessons.', CURRENT_TIMESTAMP),
(2, 3, 2, 5, 'The best Spring Boot on Kubernetes book I have read.', CURRENT_TIMESTAMP),
(3, 5, 2, 5, 'A must read for anyone who works with data systems.', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
-- Seed wishlist and followed writers for the demo customer
INSERT INTO wishlist_items (id, user_id, book_id, created_at)
VALUES (1, 1, 5, CURRENT_TIMESTAMP), (2, 1, 12, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
INSERT INTO author_follows (id, user_id, author_id, created_at)
VALUES (1, 1, 3, CURRENT_TIMESTAMP), (2, 1, 5, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
-- Seed Active Cart for Alex Rivera (User 1)
INSERT INTO carts (id, user_id, created_at, updated_at)
VALUES (1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
INSERT INTO cart_items (id, cart_id, book_id, quantity, unit_price, created_at)
VALUES
(1, 1, 1, 1, 899.00, CURRENT_TIMESTAMP),
(2, 1, 2, 1, 999.00, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
-- Seed Past Order for Alex Rivera (User 1) - eligible for Buy It Again
-- subtotal 849.00, tax 12% = 101.88, free delivery (>= 499), total 950.88, 9 points earned (1 per Rs 100)
INSERT INTO orders (id, order_number, user_id, address_id, order_status, payment_status, subtotal, tax, shipping_fee, total_amount, coupon_discount, points_redeemed, points_discount, points_earned, wallet_amount_used, payment_method, card_last4, payment_reference, estimated_delivery_date, created_at)
VALUES
(1, 'BK-20261101-440100', 1, 1, 'CONFIRMED', 'PAID', 849.00, 101.88, 0.00, 950.88, 0.00, 0, 0.00, 9, 0.00, 'CREDIT_CARD', '4242', 'CRD-SEED00000001', CURRENT_DATE, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
INSERT INTO order_items (id, order_id, book_id, book_title, book_author, cover_image_url, quantity, unit_price, total_price)
VALUES
(1, 1, 3, 'Cloud Native Spring in Action', 'Thomas Vitale', NULL, 1, 849.00, 849.00)
ON CONFLICT DO NOTHING;
-- Seed Gift Points Ledger (entries sum to the user's 250 point balance)
INSERT INTO gift_points_ledger (id, user_id, order_id, points, type, description, created_at)
VALUES
(1, 1, NULL, 200, 'EARNED', 'Welcome Sign-up Bonus', CURRENT_TIMESTAMP),
(2, 1, NULL, 41, 'EARNED', 'Review and Profile Completion Reward', CURRENT_TIMESTAMP),
(3, 1, 1, 9, 'EARNED', 'Earned from Order #BK-20261101-440100', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
INSERT INTO wallet_transactions (id, user_id, order_id, amount, type, description, created_at)
VALUES (1, 1, NULL, 1500.00, 'TOP_UP', 'Welcome wallet credit', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
-- Reset Sequences past the seeded IDs to prevent unique constraint collisions on new inserts
;

-- Move every identity sequence past the seeded ids so new rows never collide.
SELECT setval(pg_get_serial_sequence('users', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM users), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('addresses', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM addresses), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('categories', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM categories), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('publishers', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM publishers), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('authors', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM authors), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('books', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM books), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('carts', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM carts), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('cart_items', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM cart_items), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('orders', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM orders), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('order_items', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM order_items), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('gift_points_ledger', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM gift_points_ledger), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('coupons', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM coupons), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('reviews', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM reviews), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('wishlist_items', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM wishlist_items), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('author_follows', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM author_follows), 99) + 1, false);
SELECT setval(pg_get_serial_sequence('wallet_transactions', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM wallet_transactions), 99) + 1, false);
