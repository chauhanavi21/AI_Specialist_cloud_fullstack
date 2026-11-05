-- Seed Users (Password is 'Password123!')
-- BCrypt for 'Password123!': $2a$10$eYaM.av7fpnqRNOsM.ogXuXxxarwO3Fc1EnY6rSulnA.Zqd8jPhh.
INSERT INTO users (id, email, password_hash, full_name, phone_number, gift_points_balance, role, created_at)
VALUES
(1, 'alex.dev@ibm.com', '$2a$10$eYaM.av7fpnqRNOsM.ogXuXxxarwO3Fc1EnY6rSulnA.Zqd8jPhh.', 'Alex Rivera', '+1-555-0144', 250, 'ROLE_CUSTOMER', CURRENT_TIMESTAMP),
(2, 'sarah.connor@example.com', '$2a$10$eYaM.av7fpnqRNOsM.ogXuXxxarwO3Fc1EnY6rSulnA.Zqd8jPhh.', 'Sarah Connor', '+1-555-0188', 100, 'ROLE_CUSTOMER', CURRENT_TIMESTAMP);

-- Seed Addresses
INSERT INTO addresses (id, user_id, recipient_name, street_address, city, state, postal_code, country, is_default)
VALUES 
(1, 1, 'Alex Rivera', '100 IBM Innovation Way', 'Armonk', 'NY', '10504', 'United States', TRUE),
(2, 1, 'Alex Rivera (Home)', '45 Tech Plaza, Apt 4B', 'New York', 'NY', '10001', 'United States', FALSE);

-- Seed Categories
INSERT INTO categories (id, name, slug, description, book_count)
VALUES 
(1, 'Cloud & Containers', 'cloud-containers', 'Kubernetes, OpenShift, Red Hat, Docker, and hybrid cloud architecture.', 2),
(2, 'AI & Machine Learning', 'ai-ml', 'Generative AI, Agentic workflows, LLMs, and enterprise AI engineering.', 4),
(3, 'Microservices & APIs', 'microservices-apis', 'Spring Boot 3, RESTful design, event-driven architectures, and Kafka.', 3),
(4, 'DevOps & SRE', 'devops-sre', 'CI/CD pipelines, GitOps, observability, automated testing, and reliability.', 3),
(5, 'Software Engineering & Architecture', 'software-engineering', 'Domain-Driven Design, clean code, design patterns, and system design.', 3);

-- Seed Publishers (Brands)
INSERT INTO publishers (id, name, code, website)
VALUES 
(1, 'IBM Press', 'IBM_PRESS', 'https://www.ibm.com/press'),
(2, 'O''Reilly Media', 'OREILLY', 'https://www.oreilly.com'),
(3, 'Manning Publications', 'MANNING', 'https://www.manning.com'),
(4, 'Addison-Wesley', 'ADDISON_WESLEY', 'https://www.pearson.com'),
(5, 'Pragmatic Bookshelf', 'PRAGMATIC', 'https://pragprog.com');

-- Seed Books
INSERT INTO books (id, title, author, isbn, category_id, publisher_id, price, original_price, stock_quantity, rating, review_count, cover_image_url, description, published_year, page_count, tentative_delivery_days, is_featured)
VALUES 
(1, 'Enterprise Cloud Native Architecture with OpenShift', 'Dr. Roland Huemer & S. Ray', '978-0134853444', 1, 1, 54.99, 64.99, 45, 4.9, 142, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80', 'Architecting resilient, containerized mission-critical systems across hybrid multi-cloud using Red Hat OpenShift, Kubernetes, and IBM Cloud.', 2024, 480, 2, TRUE),
(2, 'Building Agentic AI Systems with LLMs', 'Elena Rostova & Avi Chauhan', '978-1492098231', 2, 2, 59.99, 69.99, 30, 4.9, 218, 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=600&auto=format&fit=crop&q=80', 'Practical handbook for building multi-agent autonomous engineering pipelines, MCP tool servers, and enterprise reasoning loops with watsonx and Bob.', 2025, 410, 2, TRUE),
(3, 'Cloud Native Spring in Action', 'Thomas Vitale', '978-1617298813', 3, 3, 49.99, 59.99, 38, 4.8, 186, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80', 'Hands-on guide to building production-grade, reactive, container-ready Spring Boot microservices with Kubernetes, PostgreSQL, and Testcontainers.', 2023, 620, 2, TRUE),
(4, 'Kubernetes Patterns & Cloud Native Best Practices', 'Bilgin Ibryam & Roland Huss', '978-1492050285', 1, 2, 47.95, 54.99, 25, 4.7, 98, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80', 'Reusable elements and design patterns for building cloud-native applications on container orchestrators.', 2023, 312, 3, FALSE),
(5, 'Designing Data-Intensive Applications', 'Martin Kleppmann', '978-1449373320', 5, 2, 44.99, 52.00, 60, 5.0, 580, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600&auto=format&fit=crop&q=80', 'The definitive guide to the storage, processing, and consensus engines behind modern high-throughput architectures.', 2022, 616, 2, TRUE),
(6, 'Generative AI on Hybrid Cloud', 'Michael J. Curry & Team', '978-0138210982', 2, 1, 52.50, 62.00, 22, 4.8, 89, 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=600&auto=format&fit=crop&q=80', 'Deploying enterprise LLMs securely on IBM Cloud, ROKS, and Red Hat Enterprise Linux with full data governance.', 2024, 390, 2, TRUE),
(7, 'Microservices Patterns with Spring Boot & Java', 'Chris Richardson', '978-1617294549', 3, 3, 51.00, 59.99, 33, 4.8, 165, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600&auto=format&fit=crop&q=80', 'Comprehensive guide to transactional sagas, API gateways, CQRS, and asynchronous event messaging.', 2023, 520, 3, FALSE),
(8, 'Site Reliability Engineering at Scale', 'Betsy Beyer & Niall Murphy', '978-1491929124', 4, 2, 48.00, 55.00, 29, 4.9, 310, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=600&auto=format&fit=crop&q=80', 'How enterprise organizations master service level objectives (SLOs), blameless postmortems, and incident response.', 2023, 550, 2, FALSE),
(9, 'Domain-Driven Design Reference & Patterns', 'Eric Evans', '978-0321125217', 5, 4, 58.00, 68.00, 18, 4.9, 420, 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=600&auto=format&fit=crop&q=80', 'Tackling software complexity at the heart of business with ubiquitous language and bounded contexts.', 2022, 560, 3, FALSE),
(10, 'Building Micro-Frontends with React & Vite', 'Luca Mezzalira', '978-1492082996', 3, 2, 42.50, 49.99, 40, 4.6, 75, 'https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?w=600&auto=format&fit=crop&q=80', 'Scaling web development across autonomous squads with modern frontend modularity and state synchronization.', 2024, 340, 2, FALSE),
(11, 'Continuous Delivery Pipelines with GitOps', 'Emily Freeman', '978-0136923485', 4, 4, 46.00, 54.00, 27, 4.7, 92, 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600&auto=format&fit=crop&q=80', 'Declarative infrastructure management, automated rollbacks, and progressive delivery using ArgoCD.', 2024, 380, 2, FALSE),
(12, 'Hands-On Large Language Models', 'Jay Alammar & Maarten Grootendorst', '978-1098150969', 2, 2, 56.00, 65.00, 35, 4.9, 195, 'https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=600&auto=format&fit=crop&q=80', 'Understanding transformer architectures, tokenization, embeddings, and fine-tuning with Python and PyTorch.', 2024, 440, 2, TRUE),
(13, 'Clean Architecture in Java 21', 'Robert C. Martin', '978-0134494166', 5, 4, 45.00, 50.00, 50, 4.8, 380, 'https://images.unsplash.com/photo-1517842645767-c639042777db?w=600&auto=format&fit=crop&q=80', 'A craftsman''s guide to software structure, dependency inversion, and testable enterprise architectures.', 2023, 432, 2, FALSE),
(14, 'Automated Testing with Testcontainers and Spring Boot', 'Kevin Wittek', '978-1617299902', 4, 3, 43.00, 49.99, 20, 4.8, 64, 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=600&auto=format&fit=crop&q=80', 'Real database and queue integration testing without fragile mocks using Docker and Testcontainers in Java.', 2024, 310, 3, FALSE),
(15, 'Prompt Engineering & Agentic Workflows for Developers', 'David Chen & Liam Vance', '978-1680509922', 2, 5, 39.99, 48.00, 42, 4.7, 110, 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80', 'Mastering system prompts, few-shot conditioning, structured schema generation, and agentic loop orchestration.', 2025, 290, 2, TRUE);

-- Seed Active Cart for Alex Rivera (User 1)
INSERT INTO carts (id, user_id, created_at, updated_at)
VALUES (1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO cart_items (id, cart_id, book_id, quantity, unit_price, created_at)
VALUES 
(1, 1, 1, 1, 54.99, CURRENT_TIMESTAMP),
(2, 1, 2, 1, 59.99, CURRENT_TIMESTAMP);

-- Seed Past Order for Alex Rivera (User 1) - eligible for Buy It Again
INSERT INTO orders (id, order_number, user_id, address_id, order_status, payment_status, subtotal, tax, shipping_fee, total_amount, points_redeemed, points_discount, points_earned, payment_method, card_last4, estimated_delivery_date, created_at)
VALUES 
(1, 'BK-20261101-4401', 1, 1, 'CONFIRMED', 'PAID', 49.99, 4.00, 0.00, 53.99, 0, 0.00, 5, 'CREDIT_CARD', '4242', CURRENT_DATE, CURRENT_TIMESTAMP);

INSERT INTO order_items (id, order_id, book_id, book_title, book_author, cover_image_url, quantity, unit_price, total_price)
VALUES 
(1, 1, 3, 'Cloud Native Spring in Action', 'Thomas Vitale', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80', 1, 49.99, 49.99);

-- Seed Gift Points Ledger
INSERT INTO gift_points_ledger (id, user_id, order_id, points, type, description, created_at)
VALUES
(1, 1, NULL, 200, 'EARNED', 'Welcome Sign-up Bonus', CURRENT_TIMESTAMP),
(2, 1, NULL, 45, 'EARNED', 'Review and Profile Completion Reward', CURRENT_TIMESTAMP),
(3, 1, 1, 5, 'EARNED', 'Earned from Order #BK-20261101-4401', CURRENT_TIMESTAMP);

-- Reset Sequences past the seeded IDs to prevent unique constraint collisions on new inserts
ALTER TABLE users ALTER COLUMN id RESTART WITH 100;
ALTER TABLE addresses ALTER COLUMN id RESTART WITH 100;
ALTER TABLE categories ALTER COLUMN id RESTART WITH 100;
ALTER TABLE publishers ALTER COLUMN id RESTART WITH 100;
ALTER TABLE books ALTER COLUMN id RESTART WITH 100;
ALTER TABLE carts ALTER COLUMN id RESTART WITH 100;
ALTER TABLE cart_items ALTER COLUMN id RESTART WITH 100;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 100;
ALTER TABLE order_items ALTER COLUMN id RESTART WITH 100;
ALTER TABLE gift_points_ledger ALTER COLUMN id RESTART WITH 100;
