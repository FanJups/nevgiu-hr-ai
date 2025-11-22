import { GeneratedJobOffer } from '../models/generated-job-offer.models';
import { JobGenerationResponse } from '../models/job-generation-response.models';
import { JobGenerationRequest } from '../models/job-generation.models';

export const MOCK_GENERATED_JOB_OFFER: GeneratedJobOffer = {
    inferredTitle: 'Senior Backend Engineer',
    level: 'Senior',
    summary: 'Design and build scalable backend services for our AI-driven finance platform. Lead technical design, ensure reliability and performance, and mentor junior engineers.',
    responsibilities: [
        'Design, implement and maintain RESTful APIs and microservices',
        'Collaborate with data scientists to productionize ML models',
        'Write high-quality, well-tested Java/Spring code',
        'Identify and resolve performance and scalability bottlenecks',
        'Participate in architectural decisions and code reviews'
    ],
    requiredQualifications: [
        '5+ years backend development experience',
        'Strong Java and Spring Boot experience',
        'Solid knowledge of relational databases (PostgreSQL)',
        'Experience with Docker and container orchestration'
    ],
    preferredQualifications: [
        'Experience with vector databases or pgvector',
        'Familiarity with OpenAI/LLM integrations',
        'Experience with observability tools (Prometheus/Grafana)'
    ],
    softSkills: [
        'Effective communication',
        'Mentorship and leadership',
        'Problem-solving and ownership',
        'Collaborative team player'
    ],
    benefits: [
        'Flexible remote-first policy',
        'Health, dental and vision insurance',
        'Competitive salary and equity',
        'Professional development budget'
    ],
    employmentType: 'Full-time',
    location: 'Remote (US-friendly)',
    salaryRange: '140000 - 180000',
    tone: 'Formal'
};

export const MOCK_JOB_GENERATION_RESPONSE: JobGenerationResponse = {
    jobOffer: {
        inferredTitle: 'Senior Backend Engineer',
        level: 'Senior',
        summary: 'Design and build scalable backend services for our AI-driven finance platform. Lead technical design, ensure reliability and performance, and mentor junior engineers.',
        responsibilities: [
            'Design, implement and maintain RESTful APIs and microservices',
            'Collaborate with data scientists to productionize ML models',
            'Write high-quality, well-tested Java/Spring code',
            'Identify and resolve performance and scalability bottlenecks',
            'Participate in architectural decisions and code reviews'
        ],
        requiredQualifications: [
            '5+ years backend development experience',
            'Strong Java and Spring Boot experience',
            'Solid knowledge of relational databases (PostgreSQL)',
            'Experience with Docker and container orchestration'
        ],
        preferredQualifications: [
            'Experience with vector databases or pgvector',
            'Familiarity with OpenAI/LLM integrations',
            'Experience with observability tools (Prometheus/Grafana)'
        ],
        softSkills: [
            'Effective communication',
            'Mentorship and leadership',
            'Problem-solving and ownership',
            'Collaborative team player'
        ],
        benefits: [
            'Flexible remote-first policy',
            'Health, dental and vision insurance',
            'Competitive salary and equity',
            'Professional development budget'
        ],
        employmentType: 'Full-time',
        location: 'Remote (US-friendly)',
        salaryRange: 'USD 140000 - 180000',
        tone: 'Professional, concise'
    } as GeneratedJobOffer,
    missingInfo: [
        'Company size / stage',
        'Exact equity details',
        'Onsite vs fully remote policy specifics'
    ],
    suggestions: [
        'Add company overview paragraph to improve employer branding',
        'Specify salary currency and range breakdown per level',
        'Include application instructions and contacts'
    ]
};

export const MOCK_JOB_GENERATION_REQUEST: JobGenerationRequest = {
    briefDescription: 'Build and maintain backend services for an AI-driven finance platform. Responsible for service design, performance, and collaborating with data scientists to productionize models.',
    department: 'Engineering',
    location: 'Remote (US)',
    employmentType: 'Full-time',
    salaryRange: 'USD 140000 - 180000',
    tone: 'Formal'
};