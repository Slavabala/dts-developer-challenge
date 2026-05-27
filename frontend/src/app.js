require('dotenv').config();

const express = require('express');
const nunjucks = require('nunjucks');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');

const taskRoutes = require('./routes/tasks');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(helmet({
    contentSecurityPolicy: {
        directives: {
            defaultSrc: ["'self'"],
            styleSrc: ["'self'", "'unsafe-inline'"],
            scriptSrc: ["'self'", "'unsafe-inline'"],
        },
    },
}));

app.use(morgan('combined'));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

app.use('/assets', express.static(
    path.join(__dirname, '../node_modules/govuk-frontend/dist/govuk/assets')
));
app.use('/assets/css', express.static(
    path.join(__dirname, '../node_modules/govuk-frontend/dist/govuk')
));
app.use('/public', express.static(path.join(__dirname, '../src/public')));

nunjucks.configure(path.join(__dirname, 'views'), {
    autoescape: true,
    express: app,
    noCache: process.env.NODE_ENV !== 'production',
});

app.set('view engine', 'html');

app.use((req, res, next) => {
    res.locals.success = typeof req.query.success === 'string' ? req.query.success : null;
    next();
});

app.use('/', taskRoutes);

app.use((req, res) => {
    res.status(404).render('error', {
        title: 'Page not found',
        message: 'The page you are looking for does not exist.',
    });
});

app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).render('error', {
        title: 'Something went wrong',
        message: 'An unexpected error occurred. Please try again.',
    });
});

if (require.main === module) {
    app.listen(PORT, () => {
        console.log(`Task management frontend running on port ${PORT}`);
    });
}

module.exports = app;
